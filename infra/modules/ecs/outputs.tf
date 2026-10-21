output "alb_dns_name" {
  description = "ALB 주소. 이 주소로 frontend / backend 에 접근한다"
  value       = aws_lb.this.dns_name
}

output "alb_arn_suffix" {
  description = "CloudWatch 알람 차원에 쓰는 ALB ARN suffix"
  value       = aws_lb.this.arn_suffix
}

output "target_group_arn_suffixes" {
  description = "CloudWatch 알람 차원에 쓰는 타겟 그룹 ARN suffix (키: frontend, backend)"
  value       = { for name, tg in aws_lb_target_group.this : name => tg.arn_suffix }
}

output "cluster_name" {
  description = "ECS 클러스터 이름"
  value       = aws_ecs_cluster.this.name
}

output "service_names" {
  description = "ECS 서비스 이름 (키: frontend, backend)"
  value       = { for name, svc in aws_ecs_service.this : name => svc.name }
}

output "log_group_names" {
  description = "CloudWatch 로그 그룹 이름 (키: frontend, backend)"
  value       = { for name, lg in aws_cloudwatch_log_group.this : name => lg.name }
}
