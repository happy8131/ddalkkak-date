variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "vpc_cidr" {
  description = "VPC CIDR. /16 이어야 서브넷 분할(/24)이 맞는다"
  type        = string
  default     = "10.0.0.0/16"
}

variable "az_count" {
  description = "사용할 가용 영역 수. ALB와 RDS 서브넷 그룹이 2개 이상을 요구한다"
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2 && var.az_count <= 3
    error_message = "az_count 는 2 또는 3 이어야 합니다."
  }
}

variable "frontend_port" {
  description = "frontend 컨테이너 포트"
  type        = number
  default     = 3000
}

variable "backend_port" {
  description = "backend 컨테이너 포트"
  type        = number
  default     = 8080
}
