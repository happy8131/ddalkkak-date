variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "github_repository" {
  description = "배포를 허용할 GitHub 저장소 (owner/repo 형식)"
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repository))
    error_message = "github_repository 는 owner/repo 형식이어야 합니다 (와일드카드 불가)."
  }
}

variable "github_environment" {
  description = "역할을 맡을 수 있는 GitHub Environment 이름. 워크플로의 environment 와 같아야 한다"
  type        = string
  default     = "prod"
}

variable "create_oidc_provider" {
  description = "GitHub OIDC 공급자를 새로 만들지 여부. 계정에 이미 있으면 false 로 두고 재사용한다"
  type        = bool
  default     = true
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
