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
