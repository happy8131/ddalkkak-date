variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "subnet_ids" {
  description = "DB 서브넷 그룹에 넣을 private 서브넷 ID 목록 (2개 AZ 이상)"
  type        = list(string)
}

variable "security_group_id" {
  description = "RDS 보안 그룹 ID"
  type        = string
}

variable "engine_version" {
  description = "PostgreSQL 버전. 메이저만 지정하면 AWS가 최신 마이너를 고른다"
  type        = string
  default     = "15"
}

variable "instance_class" {
  description = "인스턴스 클래스 (PRD: db.t3.medium)"
  type        = string
  default     = "db.t3.medium"
}

variable "db_name" {
  description = "초기 데이터베이스 이름"
  type        = string
  default     = "ddalkkak"
}

variable "master_username" {
  description = "마스터 사용자 이름. 비밀번호는 Secrets Manager 가 관리한다"
  type        = string
  default     = "ddalkkak"
}

variable "allocated_storage" {
  description = "초기 스토리지(GiB)"
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "스토리지 자동 확장 상한(GiB)"
  type        = number
  default     = 100
}

variable "multi_az" {
  description = "다중 AZ 구성 여부. 초기에는 비용 때문에 끄고 안정화 후 켠다"
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  description = "자동 백업 보관 일수"
  type        = number
  default     = 7
}
