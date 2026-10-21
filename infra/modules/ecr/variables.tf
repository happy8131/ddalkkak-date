variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "keep_image_count" {
  description = "리포지토리마다 보관할 최근 이미지 수"
  type        = number
  default     = 30
}

variable "image_tag_mutability" {
  description = "IMMUTABLE 이면 같은 태그를 덮어쓸 수 없다. git SHA 태그 전략과 맞추기 위해 기본은 IMMUTABLE"
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["IMMUTABLE", "MUTABLE"], var.image_tag_mutability)
    error_message = "image_tag_mutability 는 IMMUTABLE 또는 MUTABLE 이어야 합니다."
  }
}

variable "untagged_expire_days" {
  description = "태그 없는 이미지를 삭제하기까지의 일수"
  type        = number
  default     = 7
}
