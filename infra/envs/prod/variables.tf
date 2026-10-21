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
  description = "처음 배포할 frontend 이미지 태그. 이후 배포는 CI/CD 가 태스크 정의를 갱신한다"
  type        = string
  default     = "latest"
}

variable "backend_image_tag" {
  description = "처음 배포할 backend 이미지 태그. 이후 배포는 CI/CD 가 태스크 정의를 갱신한다"
  type        = string
  default     = "latest"
}
