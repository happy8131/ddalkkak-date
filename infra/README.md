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

모듈은 현재 뼈대(버전 고정, 공통 변수)만 있고, 실제 리소스는 후속 티켓에서 추가합니다.

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
