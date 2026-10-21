output "name_prefix" {
  description = "리소스 이름 접두사"
  value       = local.name_prefix
}

output "aws_region" {
  description = "배포 리전"
  value       = var.aws_region
}

output "alb_dns_name" {
  description = "ALB 주소. 브라우저에서 http://<주소> 로 접근한다 (/api/* 는 backend)"
  value       = module.ecs.alb_dns_name
}

output "ecr_repository_urls" {
  description = "ECR 리포지토리 URL (키: frontend, backend). 이미지를 push 할 때 쓴다"
  value       = module.ecr.repository_urls
}

output "ecs_cluster_name" {
  description = "ECS 클러스터 이름"
  value       = module.ecs.cluster_name
}

output "ecs_service_names" {
  description = "ECS 서비스 이름 (키: frontend, backend)"
  value       = module.ecs.service_names
}

output "log_group_names" {
  description = "CloudWatch 로그 그룹 이름 (키: frontend, backend)"
  value       = module.ecs.log_group_names
}

output "db_secret_arn" {
  description = "RDS 마스터 계정 Secret ARN (비밀번호 값은 출력하지 않는다)"
  value       = module.rds.master_user_secret_arn
}

output "alarm_topic_arn" {
  description = "알람 SNS 토픽 ARN"
  value       = module.observability.alarm_topic_arn
}
