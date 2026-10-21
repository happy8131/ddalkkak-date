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
