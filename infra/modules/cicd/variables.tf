variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "ecr_repository_arns" {
  description = "이미지를 push 할 ECR 리포지토리 ARN (키: frontend, backend)"
  type        = map(string)
}

variable "ecs_service_arns" {
  description = "배포할 ECS 서비스 ARN (키: frontend, backend)"
  type        = map(string)
}

variable "pass_role_arns" {
  description = "새 태스크 정의에 지정할 수 있는 IAM 역할 ARN (태스크 실행 역할, 태스크 역할)"
  type        = list(string)
}
