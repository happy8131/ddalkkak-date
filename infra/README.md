# infra (Terraform)

"데이트 딸깍"의 AWS 인프라를 Terraform으로 관리합니다. **prod 단일 환경**이고 dev 환경은 없습니다. 리전은 `us-east-1`입니다.

> `terraform apply`는 CI가 아니라 **담당자가 로컬에서 직접** 실행합니다. 인프라는 CI/CD(GitHub Actions) 대상이 아닙니다.

## 디렉터리 구조

```
infra/
├── envs/
│   └── prod/         # prod 환경 루트 모듈
└── modules/
    ├── network/      # VPC, 서브넷, 보안 그룹
    ├── ecr/          # frontend / backend 이미지 저장소
    ├── ecs/          # ECS Fargate, ALB
    ├── rds/          # RDS PostgreSQL
    └── observability/# CloudWatch 알람, SNS
```

## 만들어지는 리소스

`envs/prod`가 아래 모듈을 연결합니다. 리전은 `us-east-1`, 가용 영역은 2개를 씁니다.

| 모듈 | 리소스 |
| --- | --- |
| `network` | VPC, public / private-app / private-db 서브넷, IGW, NAT 게이트웨이, S3 VPC 엔드포인트, 보안 그룹(ALB → ECS → RDS 순으로만 허용) |
| `ecr` | `frontend`, `backend` 리포지토리 (푸시 시 이미지 스캔, 태그 변경 불가, 태그 없는 이미지 7일 뒤·나머지는 최근 30개만 보관) |
| `rds` | PostgreSQL 15 `db.t3.medium` (비공개, 암호화, 삭제 보호, 백업 7일, 단일 AZ) |
| `ecs` | ECS 클러스터(Container Insights), frontend / backend Fargate 서비스, ALB, CloudWatch 로그 그룹 |
| `observability` | SNS 알람 토픽, ALB 5xx 비율 / 비정상 대상 / ECS CPU·메모리 / RDS CPU·연결 수 알람 |

- **라우팅:** ALB 80번 포트에서 `/api/*`는 backend(8080), 그 외는 frontend(3000)로 보냅니다. 백엔드 헬스체크(`/actuator/health`)는 ALB가 타겟에 직접 호출하며 외부에는 노출되지 않습니다.
- **비밀 값:** RDS가 마스터 비밀번호를 Secrets Manager에 직접 만들고 관리합니다(`manage_master_user_password`). 비밀번호는 코드, tfvars, Terraform 변수, state에 나타나지 않고, ECS가 컨테이너 시작 시점에 `DB_USERNAME`, `DB_PASSWORD`로 주입합니다.
- **알람:** `alarm_email`을 지정하면 SNS 이메일 구독이 만들어지고, 받은 메일의 확인 링크를 눌러야 활성화됩니다. 비워 두면 토픽만 만들어집니다.
- **배포:** 서비스는 `task_definition` 변경을 무시합니다(`ignore_changes`). 이후 이미지 배포는 CI/CD가 태스크 정의를 갱신하는 방식이라 Terraform이 되돌리지 않습니다.

### 알아 둘 점

- **비용:** NAT 게이트웨이, ALB, RDS `db.t3.medium`이 상시 비용입니다.
- **NAT 가용성 / 비용 선택:** 기본은 NAT 1개(`nat_per_az = false`)입니다. 이 경우 NAT가 있는 AZ에 장애가 나면 private 서브넷의 아웃바운드(이미지 pull 등)가 모두 끊깁니다. 가용성이 더 중요해지면 `nat_per_az = true`로 AZ마다 NAT를 두며 NAT 비용이 AZ 수만큼 늘어납니다. 현재 `envs/prod`는 기본값을 쓰고, 바꾸려면 `module "network"`에 `nat_per_az = true`를 넣습니다.
- **VPC 엔드포인트:** S3 게이트웨이 엔드포인트는 무료라서 항상 켜 둡니다(ECR 이미지 레이어가 S3에서 내려오므로 NAT 트래픽이 줄어듭니다). ECR(api, dkr), CloudWatch Logs, Secrets Manager 인터페이스 엔드포인트는 AZ마다 시간당 요금이 붙어서 기본은 끄고, NAT 데이터 처리 비용이 커질 때 `enable_interface_endpoints = true`로 켭니다.
- **보안 그룹:** ALB는 80만 공개합니다. HTTPS 리스너(ACM)를 붙일 때 `alb_https_enabled = true`로 443을 엽니다.
- **HTTP만 지원합니다.** 도메인과 HTTPS(ACM), CloudFront는 초기 배포가 안정된 뒤 별도 작업으로 추가합니다. CORS 허용 출처는 ALB 주소(`http://<ALB DNS>`)입니다.
- **프론트엔드 API 주소:** 프론트엔드 이미지는 빌드 시점에 `NEXT_PUBLIC_API_BASE_URL`이 고정됩니다. ALB 주소가 정해진 뒤 그 값으로 이미지를 빌드해야 브라우저가 backend를 호출할 수 있습니다.
- **Redis, 장소 수집 배치(ECS Scheduled Task):** 이번 범위에 포함하지 않았습니다.

## 첫 배포 순서

ECR 태그는 **변경 불가(IMMUTABLE)**라서 `latest`처럼 같은 태그를 다시 push할 수 없고, 이미지는 항상 git SHA 태그로 올립니다. 서비스 생성 시점의 태그(`bootstrap`)는 자리표시일 뿐이며 실제 이미지는 배포 워크플로가 올립니다.

1. 아래 "init / plan / apply"대로 `apply`해서 인프라를 만듭니다. 이 시점에는 ECR이 비어 있어서 ECS 태스크는 이미지를 받지 못해 기동에 실패합니다(정상입니다).
2. "CI/CD" 절의 순서대로 GitHub Environment `prod`에 `AWS_DEPLOY_ROLE_ARN`과 `NEXT_PUBLIC_API_BASE_URL`(`terraform output alb_dns_name`의 주소)을 등록합니다.
3. GitHub Actions에서 `Deploy Backend`, `Deploy Frontend`를 각각 **Run workflow**(workflow_dispatch)로 실행합니다. 이미지가 git SHA 태그로 ECR에 올라가고 ECS 서비스가 새 태스크 정의로 배포됩니다.
4. `http://<ALB DNS>/`(프론트엔드)와 `http://<ALB DNS>/api/v1/...`(백엔드)로 확인하고, CloudWatch Logs `/ecs/ddalkkak-date-prod/*`에서 로그를 봅니다.

수동으로 push해야 할 때는 SHA처럼 매번 다른 태그를 씁니다. 같은 태그를 다시 push하면 `ImageTagAlreadyExistsException`으로 실패합니다.

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <계정 ID>.dkr.ecr.us-east-1.amazonaws.com
docker build -t <repository_url>:$(git rev-parse HEAD) backend
docker push <repository_url>:$(git rev-parse HEAD)
```

## 테스트 (AWS 호출 없음)

AWS 자격 증명 없이도 구성을 검증할 수 있습니다. mock provider로 `apply`를 흉내 내며 실제 계정에는 아무것도 만들지 않습니다.

```bash
cd infra/envs/prod
terraform init
terraform test
```

`tests/modules.tftest.hcl`은 모듈 내부 설정(경로 라우팅, 비밀 값 주입 방식, RDS 비공개·암호화, 알람 임계치 등)을, `tests/plan.tftest.hcl`은 모듈 연결과 태그를 확인합니다. 이 테스트가 통과해도 **실제 AWS에서의 동작은 검증된 것이 아닙니다.** 실제 `plan` 결과는 아래 절차대로 직접 확인합니다.

## 상태(state) 관리

S3 백엔드(원격 상태)는 쓰지 않습니다. 상태는 `infra/envs/prod/terraform.tfstate`에 **로컬 파일**로 저장됩니다.

- `*.tfstate*`는 `.gitignore`로 제외되어 커밋되지 않습니다. 상태에는 비밀 값이 들어갈 수 있으니 공유 저장소나 메신저에 올리지 마세요.
- 상태 파일을 잃으면 Terraform이 기존 리소스를 알아보지 못하므로, apply한 뒤에는 `terraform.tfstate`를 별도로 백업해 두세요.
- 로컬 상태는 잠금이 없어서 **한 사람만** apply해야 합니다. 여럿이 함께 쓰게 되면 원격 상태 도입을 다시 검토합니다.

## 사전 준비

- Terraform `>= 1.10, < 2.0`, AWS provider `~> 6.0`
- AWS CLI v2

### AWS 인증 (장기 액세스 키 사용 금지)

`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` 같은 장기 키는 쓰지 않고, **IAM Identity Center(SSO) 프로파일**로 로그인합니다.

```bash
aws configure sso                    # 최초 1회: 프로파일 이름 예) ddalkkak-prod
aws sso login --profile ddalkkak-prod
export AWS_PROFILE=ddalkkak-prod     # PowerShell: $env:AWS_PROFILE = "ddalkkak-prod"
aws sts get-caller-identity          # 올바른 계정인지 반드시 확인
```

코드와 `tfvars`에는 자격 증명을 넣지 않습니다. 세션이 만료되면 `aws sso login`을 다시 실행합니다.

## init / plan / apply

```bash
cd infra/envs/prod
cp terraform.tfvars.example terraform.tfvars   # owner 수정
terraform init
terraform fmt -recursive
terraform validate
terraform plan -out=tfplan
```

### apply 전 확인 절차 (prod 실수 방지)

prod에는 환경이 하나뿐이라 잘못 apply하면 바로 운영에 영향이 갑니다. 아래를 지킵니다.

1. `aws sts get-caller-identity`로 **계정 ID와 프로파일**이 의도한 prod인지 확인합니다.
2. `terraform plan -out=tfplan`을 실행하고 **출력 전체를 읽습니다.** 특히 `destroy`, `replace`(`-/+`), `forces replacement`가 있는지 봅니다.
3. 예상하지 못한 변경이 있으면 apply하지 않고 원인부터 찾습니다.
4. 검토한 plan 파일 그대로 적용합니다. 새로 계산한 plan으로 적용하지 않습니다.
   ```bash
   terraform apply tfplan
   ```
   `terraform apply -auto-approve`는 쓰지 않습니다.
5. 삭제나 교체가 포함된 plan은 다른 팀원에게 plan 출력을 공유해 확인받은 뒤 적용합니다.

## 규칙

- **공통 태그**: 모든 리소스에 `project`, `env=prod`, `owner`가 provider `default_tags`로 자동 적용됩니다. 값은 `variables.tf`(`project`, `env`, `owner`)에서 정합니다.
- **버전 고정**: `required_version`과 provider 버전을 고정하고, `.terraform.lock.hcl`은 커밋합니다(windows/linux/macOS 해시 포함). provider를 올릴 때는 `terraform init -upgrade` 후 lock 파일 변경분을 함께 커밋합니다.
- **커밋하지 않는 파일**: `.terraform/`, `*.tfstate*`, `*.tfvars`, `*tfplan*` (`infra/.gitignore`). 예시 파일(`terraform.tfvars.example`)만 커밋합니다.
- **비밀 값**: DB 비밀번호, API 키 등은 `tfvars`나 코드에 넣지 않고 Secrets Manager / SSM Parameter Store를 씁니다.
- AWS 콘솔에서 리소스를 수동으로 바꾸지 않습니다. 모든 변경은 Terraform으로 합니다.
- 포맷과 검증: 커밋 전에 `terraform fmt -recursive`와 `terraform validate`를 통과시킵니다.
