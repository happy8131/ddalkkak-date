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
    ├── secrets/      # 외부 API 키 자리 (SSM SecureString)
    ├── observability/# CloudWatch 알람, SNS
    └── cicd/         # GitHub Actions 배포용 IAM 사용자
```

## 만들어지는 리소스

`envs/prod`가 아래 모듈을 연결합니다. 리전은 `us-east-1`, 가용 영역은 2개를 씁니다.

| 모듈 | 리소스 |
| --- | --- |
| `network` | VPC, public / private-app / private-db 서브넷, IGW, NAT 게이트웨이(선택), S3 VPC 엔드포인트, 보안 그룹(ALB → ECS → RDS 순으로만 허용) |
| `ecr` | `frontend`, `backend` 리포지토리 (푸시 시 이미지 스캔, 태그 변경 불가, 태그 없는 이미지 7일 뒤·나머지는 최근 30개만 보관) |
| `rds` | PostgreSQL 15 `db.t3.medium` (비공개, 암호화, 삭제 보호, 백업 7일, 단일 AZ) |
| `secrets` | 외부 API 키 자리 SSM 파라미터 5개(카카오, LLM, Anthropic, 네이버 ID/Secret). 값은 placeholder이고 별도로 주입 |
| `ecs` | ECS 클러스터(Container Insights), frontend / backend Fargate 서비스(CPU 기반 Auto Scaling), ALB, CloudWatch 로그 그룹, 태스크 실행·태스크 역할 |
| `observability` | SNS 알람 토픽, ALB 5xx 비율 / 비정상 대상 / ECS CPU·메모리 / RDS CPU·연결 수 알람 |
| `cicd` | 배포 전용 IAM 사용자 (ECR push, ECS 서비스 갱신 권한만. 액세스 키는 Terraform 밖에서 발급) |

- **라우팅:** ALB 80번 포트에서 `/api/*`는 backend(8080), 그 외는 frontend(3000)로 보냅니다. 백엔드 헬스체크(`/actuator/health`)는 ALB가 타겟에 직접 호출하며 외부에는 노출되지 않습니다.
- **비밀 값:** RDS가 마스터 비밀번호를 Secrets Manager에 직접 만들고 관리합니다(`manage_master_user_password`). 비밀번호는 코드, tfvars, Terraform 변수, state에 나타나지 않고, ECS가 컨테이너 시작 시점에 `DB_USERNAME`, `DB_PASSWORD`로 주입합니다.
- **알람:** `alarm_email`을 지정하면 SNS 이메일 구독이 만들어지고, 받은 메일의 확인 링크를 눌러야 활성화됩니다. 비워 두면 토픽만 만들어집니다.
- **배포:** 서비스는 `task_definition`과 `desired_count` 변경을 무시합니다(`ignore_changes`). 이미지 배포는 태스크 정의를 따로 갱신하는 방식이라 Terraform이 되돌리지 않고(GitHub Actions가 태스크 정의를 갱신합니다. 아래 "CI/CD" 참고), 태스크 수는 Auto Scaling이 조정하므로 처음 생성할 때의 값만 씁니다. 배포가 실패하면 Deployment Circuit Breaker가 이전 버전으로 되돌립니다.
- **헬스체크:** ALB 타겟 그룹(frontend `/`, backend `/actuator/health`)과 컨테이너 헬스체크(task definition)를 둘 다 둡니다. frontend 이미지(`node:slim`)에는 `curl`이 없어서 Node 내장 `fetch`로 확인하고, backend는 `curl`로 확인합니다. backend는 Spring Boot와 Flyway 기동 시간을 위해 시작 유예 시간(120초)을 둡니다.
- **Auto Scaling:** 서비스마다 평균 CPU 70%를 목표로 하는 목표 추적 정책이며, 초기 범위는 최소 1 / 최대 2개입니다(`*_min_capacity`, `*_max_capacity`로 조정).
- **IAM 최소 권한:** 태스크 실행 역할은 관리형 `AmazonECSTaskExecutionRolePolicy`(로그·ECR이 모든 리소스에 열려 있음) 대신, 이 스택의 ECR 리포지토리 pull, 로그 그룹 쓰기, DB Secret 읽기만 허용하는 정책을 직접 둡니다. 태스크 역할은 애플리케이션이 AWS API를 쓰지 않아 권한이 없습니다.

### DB와 시크릿

- **인스턴스 크기:** PRD 기준 `db.t3.medium`입니다. 바꾸려면 `db_instance_class`를 지정합니다.
- **Multi-AZ:** 기본은 단일 AZ(`db_multi_az = false`)입니다. Multi-AZ는 장애 시 자동 전환되지만 RDS 비용이 대략 두 배가 됩니다. 실사용자가 생기기 전에 `db_multi_az = true`로 켜는 것을 권장하며, 켜면 짧은 중단이 있을 수 있어 plan을 먼저 확인합니다.
- **보호:** 삭제 보호와 최종 스냅샷이 켜져 있습니다. 인스턴스를 정말 지우려면 `deletion_protection`을 먼저 끄는 apply가 필요합니다.
- **DB 계정:** RDS가 Secrets Manager에 마스터 계정을 만들고 관리합니다. ECS가 컨테이너 시작 시 `DB_USERNAME`, `DB_PASSWORD`로 주입하며 코드와 tfvars에는 평문이 없습니다. `DB_URL`은 호스트 주소뿐이라 비밀이 아니므로 일반 환경 변수로 넣습니다.
- **외부 API 키:** `secrets` 모듈이 `/ddalkkak-date-prod/<키>` 이름의 SecureString 파라미터를 placeholder(`REPLACE_ME`)로 만들어 둡니다. 실제 값은 apply 후 콘솔(Systems Manager → Parameter Store)에서 넣는 것을 권장합니다. CLI를 쓰면 값이 셸 기록에 남으니 주의하세요.
  ```bash
  aws ssm put-parameter --name /ddalkkak-date-prod/kakao-rest-api-key --type SecureString --overwrite --value "<값>"
  ```
  이후 apply가 주입한 값을 placeholder로 되돌리지 않습니다(`ignore_changes`). 이 키들은 아직 어떤 ECS 태스크에도 주입하지 않습니다. 사용하는 태스크(예: 장소 수집 배치)를 만들 때 `secrets`에 `parameter_arns`를 연결하고 실행 역할에 `ssm:GetParameters` 권한을 추가합니다.
- **네이버 키:** 장소 수집에서는 네이버 지역 검색을 쓰지 않지만, PRD의 블로그 후기 검색용으로 자리만 만들어 두었습니다.

### NAT 없이 쓰는 구성 (비용 절감)

NAT 게이트웨이는 켜 두면 월 약 $35가 들고 중지할 수 없어서(삭제만 가능), 테스트 환경에서는 가장 큰 고정비입니다. `terraform.tfvars`에 아래 한 줄을 넣으면 NAT와 고정 IP를 만들지 않습니다.

```hcl
enable_nat_gateway = false
```

- ECS 태스크가 **퍼블릭 서브넷에서 공인 IP를 받아** 직접 인터넷으로 나갑니다(ECR 이미지 pull, 외부 API 호출). NAT만 끄고 태스크를 프라이빗에 두면 이미지를 받지 못하므로, 두 설정은 이 변수 하나로 함께 전환됩니다.
- 보안 경계는 유지됩니다. 태스크 보안 그룹은 ALB에서 오는 트래픽만 허용하고, RDS는 계속 프라이빗이며 태스크에서만 접근됩니다. 태스크 공인 IP로 앱 포트(3000, 8080)에 직접 접속해도 연결되지 않는 것을 확인했습니다.
- 공인 IPv4는 개당 월 약 $3.6이라 태스크 2개면 약 $7이 듭니다. NAT 절감분(약 $35)에서 빼면 순절감은 약 $28입니다. 태스크 수가 늘수록 줄어듭니다.
- 외부 API 허용 목록에 쓸 고정 출구 IP가 없어집니다. 태스크가 교체될 때마다 공인 IP가 바뀝니다.
- 이미 NAT가 있는 환경에서 끄면 프라이빗 라우트 테이블에 NAT를 가리키는 `blackhole` 경로가 남을 수 있습니다. 라우트 테이블의 `route`는 provider가 계산하는 속성이라, 블록을 없애도 기존 경로를 지우지 않기 때문입니다. 태스크가 퍼블릭 서브넷을 쓰므로 동작에는 영향이 없습니다. 다시 NAT를 켜면 새 NAT를 가리키도록 갱신됩니다.
- 실서비스에서는 다시 `true`(기본값)로 두고 태스크를 프라이빗 서브넷에 두는 구성을 권장합니다.

### 알아 둘 점

- **비용:** NAT 게이트웨이(끌 수 있음, 위 "NAT 없이 쓰는 구성" 참고), ALB, RDS가 상시 비용입니다.
- **NAT 가용성 / 비용 선택:** 기본은 NAT 1개(`nat_per_az = false`)입니다. 이 경우 NAT가 있는 AZ에 장애가 나면 private 서브넷의 아웃바운드(이미지 pull 등)가 모두 끊깁니다. 가용성이 더 중요해지면 `nat_per_az = true`로 AZ마다 NAT를 두며 NAT 비용이 AZ 수만큼 늘어납니다. 현재 `envs/prod`는 기본값을 쓰고, 바꾸려면 `module "network"`에 `nat_per_az = true`를 넣습니다.
- **VPC 엔드포인트:** S3 게이트웨이 엔드포인트는 무료라서 항상 켜 둡니다(ECR 이미지 레이어가 S3에서 내려오므로 NAT 트래픽이 줄어듭니다). ECR(api, dkr), CloudWatch Logs, Secrets Manager 인터페이스 엔드포인트는 AZ마다 시간당 요금이 붙어서 기본은 끄고, NAT 데이터 처리 비용이 커질 때 `enable_interface_endpoints = true`로 켭니다.
- **보안 그룹:** ALB는 80만 공개합니다. HTTPS 리스너(ACM)를 붙일 때 `alb_https_enabled = true`로 443을 엽니다.
- **HTTP만 지원합니다.** 도메인과 HTTPS(ACM), CloudFront는 초기 배포가 안정된 뒤 별도 작업으로 추가합니다. CORS 허용 출처는 ALB 주소(`http://<ALB DNS>`)입니다.
- **프론트엔드 API 주소:** 프론트엔드 이미지는 빌드 시점에 `NEXT_PUBLIC_API_BASE_URL`이 고정됩니다. ALB 주소가 정해진 뒤 그 값으로 이미지를 빌드해야 브라우저가 backend를 호출할 수 있습니다.
- **Redis, 장소 수집 배치(ECS Scheduled Task):** 이번 범위에 포함하지 않았습니다.

## 첫 배포 순서

ECR 태그는 **변경 불가(IMMUTABLE)**라서 `latest`처럼 같은 태그를 다시 push할 수 없고, 이미지는 매번 다른 태그(git SHA)로 올립니다. 서비스 생성 시점의 태그(`bootstrap`)는 자리표시일 뿐입니다. 서비스는 `task_definition` 변경을 무시(`ignore_changes`)하므로, 태그를 바꿔 apply한 뒤에는 서비스를 새 태스크 정의로 직접 갱신해야 합니다. 자동 배포 파이프라인(CI/CD)은 아직 없습니다.

1. 위 "init / plan / apply"대로 `apply`해서 인프라를 만듭니다. 이 시점에는 ECR이 비어 있어서 ECS 태스크는 이미지를 받지 못해 기동에 실패합니다(정상입니다).
2. `terraform output alb_dns_name`으로 주소를 확인하고, 그 주소를 `NEXT_PUBLIC_API_BASE_URL`로 넣어 이미지를 빌드해 push합니다. 태그는 매번 달라야 합니다.
   ```bash
   TAG=$(git rev-parse --short HEAD)
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <계정 ID>.dkr.ecr.us-east-1.amazonaws.com
   docker build -t <backend_repository_url>:$TAG backend
   docker build --build-arg NEXT_PUBLIC_API_BASE_URL=http://<ALB DNS> -t <frontend_repository_url>:$TAG frontend
   docker push <backend_repository_url>:$TAG
   docker push <frontend_repository_url>:$TAG
   ```
   `<...repository_url>`은 `terraform output ecr_repository_urls`에서 확인합니다.
3. 새 태그로 태스크 정의를 만듭니다. plan을 확인한 뒤 apply합니다.
   ```bash
   terraform plan -out=tfplan -var backend_image_tag=$TAG -var frontend_image_tag=$TAG
   terraform apply tfplan
   ```
4. 서비스를 새 태스크 정의(가장 최근 리비전)로 갱신합니다. 서비스 이름은 `terraform output ecs_service_names`로 확인합니다.
   ```bash
   aws ecs update-service --cluster <ecs_cluster_name> --service <서비스 이름> --task-definition <패밀리 이름> --force-new-deployment
   ```
   패밀리 이름은 `ddalkkak-date-prod-backend`, `ddalkkak-date-prod-frontend`입니다.
5. `http://<ALB DNS>/`(프론트엔드)와 `http://<ALB DNS>/api/v1/...`(백엔드)로 확인하고, CloudWatch Logs `/ecs/ddalkkak-date-prod/*`에서 로그를 봅니다.

같은 태그를 다시 push하면 `ImageTagAlreadyExistsException`으로 실패합니다. 코드를 고쳐 다시 배포할 때는 새 태그로 2~4번을 반복합니다.

## CI/CD (GitHub Actions) 연결

워크플로는 `.github/workflows/`에 있습니다. 인프라(Terraform)는 CI/CD 대상이 아니라서 plan/apply는 여기에 포함되지 않습니다.

| 워크플로 | 시점 | 하는 일 |
| --- | --- | --- |
| `ci-frontend.yml` | `frontend/**` 변경 PR | `npm ci`, lint, build |
| `ci-backend.yml` | `backend/**` 변경 PR | `./gradlew test bootJar` |
| `deploy-frontend.yml` | `main`에 `frontend/**` 변경 머지 | 이미지 빌드 → ECR push(태그: git SHA) → 태스크 정의 갱신 → 롤링 배포 |
| `deploy-backend.yml` | `main`에 `backend/**` 변경 머지 | 위와 같음 (backend) |

배포 워크플로는 서비스가 안정화될 때까지 기다리고, 실패하면 워크플로도 실패합니다. 새 태스크가 헬스체크를 통과하지 못하면 ECS Deployment Circuit Breaker가 이전 버전으로 되돌립니다.

인증은 **배포 전용 IAM 사용자의 액세스 키**로 합니다. 사용자(`cicd` 모듈)의 권한은 ECR push와 ECS 서비스 갱신으로 한정되고 Terraform 인프라 변경 권한은 없습니다.

- **키는 Terraform으로 만들지 않습니다.** `aws_iam_access_key`를 쓰면 시크릿 키가 `terraform.tfstate`에 평문으로 남기 때문에, 사용자만 만들고 키는 콘솔이나 CLI로 따로 발급합니다.
- 장기 키라서 유출되면 만료되지 않습니다. 정기적으로 교체(rotate)하고, 사용자에게는 이 권한 외에 다른 정책을 붙이지 마세요.

### 최초 설정 순서

1. `terraform apply` 후 `terraform output github_deploy_user_name`으로 사용자 이름을 확인합니다.
2. 그 사용자의 액세스 키를 발급합니다. 콘솔(IAM → 사용자 → 보안 자격 증명 → 액세스 키 만들기)을 권장합니다. 시크릿 키는 발급 화면에서 한 번만 볼 수 있습니다.
   ```bash
   aws iam create-access-key --user-name ddalkkak-date-prod-github-deploy
   ```
   CLI를 쓰면 출력이 셸 기록과 터미널에 남으니 주의하세요.
3. GitHub 저장소 → Settings → Environments → **prod**를 만들고 다음을 등록합니다.
   - Secret `AWS_ACCESS_KEY_ID`: 발급한 액세스 키 ID
   - Secret `AWS_SECRET_ACCESS_KEY`: 발급한 시크릿 액세스 키
   - Variable `NEXT_PUBLIC_API_BASE_URL`: 브라우저가 backend를 호출할 주소 (예: `http://<ALB DNS>`). frontend 이미지 빌드 시점에 고정됩니다.
   - (권장) Deployment branches를 `main`으로 제한합니다. 시크릿이 `prod` 환경에 묶여 있어, 이 설정이 PR 브랜치에서 키를 쓰지 못하게 하는 방어선입니다.
4. 워크플로가 쓰는 이름(클러스터, 서비스, ECR 리포지토리)은 `ddalkkak-date-prod-*`로 고정되어 있습니다. `project`나 `env`를 바꾸면 `deploy-*.yml`의 `env` 값도 함께 바꿔야 합니다(`terraform test`가 클러스터·서비스 이름이 어긋나면 실패합니다).

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
