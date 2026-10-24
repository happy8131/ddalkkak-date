variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "프로젝트 이름 (리소스 이름과 project 태그에 쓴다)"
  type        = string
  default     = "ddalkkak-date"
}

variable "env" {
  description = "환경 이름. prod 단일 환경만 쓰므로 다른 값은 허용하지 않는다"
  type        = string
  default     = "prod"

  validation {
    condition     = var.env == "prod"
    error_message = "env 는 prod 만 허용합니다 (dev 환경은 두지 않습니다)."
  }
}

variable "owner" {
  description = "리소스 담당자 (owner 태그). terraform.tfvars 에서 지정한다"
  type        = string
}

variable "alarm_email" {
  description = "CloudWatch 알람을 받을 이메일. 비우면 SNS 구독을 만들지 않는다"
  type        = string
  default     = ""
}

variable "db_multi_az" {
  description = "RDS 다중 AZ 여부. 초기에는 비용 때문에 끄고 안정화 후 켠다"
  type        = bool
  default     = false
}

variable "frontend_image_tag" {
  description = "frontend 초기 이미지 태그. 생성 시점의 자리표시 값이며, 실제 배포는 CI/CD 가 git SHA 태그로 태스크 정의를 갱신한다"
  type        = string
  default     = "bootstrap"
}

variable "backend_image_tag" {
  description = "backend 초기 이미지 태그. 생성 시점의 자리표시 값이며, 실제 배포는 CI/CD 가 git SHA 태그로 태스크 정의를 갱신한다"
  type        = string
  default     = "bootstrap"
}

variable "db_instance_class" {
  description = "RDS 인스턴스 클래스 (PRD: db.t3.medium)"
  type        = string
  default     = "db.t3.medium"
}

variable "db_backup_retention_days" {
  description = "RDS 자동 백업 보관 일수. AWS 무료 플랜 계정은 1일까지만 허용된다 (유료 계정은 7 권장)"
  type        = number
  default     = 7
}

variable "enable_nat_gateway" {
  description = "false 면 NAT 게이트웨이를 만들지 않고 ECS 태스크를 공인 IP 가 붙는 퍼블릭 서브넷에 둔다(월 약 $35 절감, 태스크가 인터넷에 직접 노출되는 대신 인바운드는 ALB 만 허용)"
  type        = bool
  default     = true
}
