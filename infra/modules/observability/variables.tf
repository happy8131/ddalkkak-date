variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod)"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "alarm_email" {
  description = "알람을 받을 이메일. 비우면 SNS 구독을 만들지 않는다"
  type        = string
  default     = ""
}

variable "alb_arn_suffix" {
  description = "ALB ARN suffix (ecs 모듈 출력)"
  type        = string
}

variable "target_group_arn_suffixes" {
  description = "타겟 그룹 ARN suffix (키: frontend, backend)"
  type        = map(string)
}

variable "cluster_name" {
  description = "ECS 클러스터 이름"
  type        = string
}

variable "service_names" {
  description = "ECS 서비스 이름 (키: frontend, backend)"
  type        = map(string)
}

variable "db_identifier" {
  description = "RDS 인스턴스 식별자"
  type        = string
}

variable "alb_5xx_rate_threshold" {
  description = "ALB 5xx 비율 임계치(%)"
  type        = number
  default     = 5
}

variable "ecs_utilization_threshold" {
  description = "ECS CPU / 메모리 사용률 임계치(%)"
  type        = number
  default     = 80
}

variable "rds_cpu_threshold" {
  description = "RDS CPU 사용률 임계치(%)"
  type        = number
  default     = 80
}

variable "rds_connections_threshold" {
  description = "RDS 연결 수 임계치. db.t3.medium 의 최대 연결 수(약 400)를 고려한 값"
  type        = number
  default     = 200
}
