output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "ALB 용 public 서브넷 ID 목록"
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "ECS 태스크 용 private 서브넷 ID 목록"
  value       = aws_subnet.private_app[*].id
}

output "private_db_subnet_ids" {
  description = "RDS 용 private 서브넷 ID 목록"
  value       = aws_subnet.private_db[*].id
}

output "alb_security_group_id" {
  description = "ALB 보안 그룹 ID"
  value       = aws_security_group.alb.id
}

output "app_security_group_id" {
  description = "ECS 태스크 보안 그룹 ID"
  value       = aws_security_group.app.id
}

output "rds_security_group_id" {
  description = "RDS 보안 그룹 ID"
  value       = aws_security_group.rds.id
}
