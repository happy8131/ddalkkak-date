output "name_prefix" {
  description = "리소스 이름 접두사"
  value       = local.name_prefix
}

output "aws_region" {
  description = "배포 리전"
  value       = var.aws_region
}
