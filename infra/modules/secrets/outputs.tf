output "parameter_arns" {
  description = "SSM 파라미터 ARN (키: 파라미터 키). ECS 태스크 정의의 secrets valueFrom 에 쓴다"
  value       = { for key, p in aws_ssm_parameter.this : key => p.arn }
}

output "parameter_names" {
  description = "SSM 파라미터 이름 (키: 파라미터 키). 값을 주입할 때 쓴다"
  value       = { for key, p in aws_ssm_parameter.this : key => p.name }
}
