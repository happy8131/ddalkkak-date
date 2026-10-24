variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "aws_region" {
  description = "CloudWatch Logs 드라이버에 넘길 리전"
  type        = string
}

# --- 네트워크 (network 모듈 출력) ---

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "public_subnet_ids" {
  description = "ALB 용 public 서브넷 ID 목록"
  type        = list(string)
}

variable "private_app_subnet_ids" {
  description = "ECS 태스크 용 private 서브넷 ID 목록"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "ALB 보안 그룹 ID"
  type        = string
}

variable "app_security_group_id" {
  description = "ECS 태스크 보안 그룹 ID"
  type        = string
}

# --- 이미지 (ecr 모듈 출력) ---

variable "repository_urls" {
  description = "ECR 리포지토리 URL (키: frontend, backend)"
  type        = map(string)
}

variable "frontend_image_tag" {
  description = "frontend 이미지 태그. 생성 시점의 자리표시 값이며, 실제 배포는 CI/CD 가 git SHA 태그로 태스크 정의를 갱신한다"
  type        = string
  default     = "bootstrap"
}

variable "backend_image_tag" {
  description = "backend 이미지 태그. 생성 시점의 자리표시 값이며, 실제 배포는 CI/CD 가 git SHA 태그로 태스크 정의를 갱신한다"
  type        = string
  default     = "bootstrap"
}

# --- DB (rds 모듈 출력) ---

variable "db_address" {
  description = "DB 엔드포인트 호스트 이름"
  type        = string
}

variable "db_port" {
  description = "DB 포트"
  type        = number
}

variable "db_name" {
  description = "데이터베이스 이름"
  type        = string
}

variable "db_secret_arn" {
  description = "RDS가 관리하는 마스터 계정 Secret ARN"
  type        = string
}

# --- 크기 ---

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

variable "frontend_cpu" {
  description = "frontend 태스크 CPU 단위 (256 = 0.25 vCPU)"
  type        = number
  default     = 256
}

variable "frontend_memory" {
  description = "frontend 태스크 메모리(MiB)"
  type        = number
  default     = 512
}

variable "backend_cpu" {
  description = "backend 태스크 CPU 단위 (512 = 0.5 vCPU)"
  type        = number
  default     = 512
}

variable "backend_memory" {
  description = "backend 태스크 메모리(MiB)"
  type        = number
  default     = 1024
}

variable "frontend_desired_count" {
  description = "frontend 태스크 수"
  type        = number
  default     = 1
}

variable "backend_desired_count" {
  description = "backend 태스크 수"
  type        = number
  default     = 1
}

variable "cpu_architecture" {
  description = "태스크 CPU 아키텍처. 이미지를 빌드한 아키텍처와 같아야 한다"
  type        = string
  default     = "X86_64"

  validation {
    condition     = contains(["X86_64", "ARM64"], var.cpu_architecture)
    error_message = "cpu_architecture 는 X86_64 또는 ARM64 여야 합니다."
  }
}

variable "log_retention_days" {
  description = "CloudWatch Logs 보관 일수"
  type        = number
  default     = 30
}

variable "repository_arns" {
  description = "ECR 리포지토리 ARN (키: frontend, backend). 태스크 실행 역할의 이미지 pull 권한 범위다"
  type        = map(string)
}

variable "frontend_min_capacity" {
  description = "frontend Auto Scaling 최소 태스크 수"
  type        = number
  default     = 1
}

variable "frontend_max_capacity" {
  description = "frontend Auto Scaling 최대 태스크 수"
  type        = number
  default     = 2
}

variable "backend_min_capacity" {
  description = "backend Auto Scaling 최소 태스크 수"
  type        = number
  default     = 1
}

variable "backend_max_capacity" {
  description = "backend Auto Scaling 최대 태스크 수"
  type        = number
  default     = 2
}

variable "autoscaling_cpu_target" {
  description = "Auto Scaling 목표 평균 CPU 사용률(%)"
  type        = number
  default     = 70
}

variable "assign_public_ip" {
  description = "true 면 태스크를 퍼블릭 서브넷에 두고 공인 IP 를 붙인다(NAT 없이 이미지 pull / 외부 API 호출). false 면 프라이빗 서브넷 + NAT"
  type        = bool
  default     = false
}
