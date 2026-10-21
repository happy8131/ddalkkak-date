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

output "nat_gateway_ids" {
  description = "NAT 게이트웨이 ID 목록 (nat_per_az 가 false 면 1개)"
  value       = aws_nat_gateway.this[*].id
}

output "nat_public_ips" {
  description = "NAT 게이트웨이의 고정 공인 IP. 외부 API 허용 목록에 등록할 때 쓴다"
  value       = aws_eip.nat[*].public_ip
}

output "s3_endpoint_id" {
  description = "S3 게이트웨이 VPC 엔드포인트 ID"
  value       = aws_vpc_endpoint.s3.id
}

output "interface_endpoint_ids" {
  description = "인터페이스 VPC 엔드포인트 ID (키: 서비스 이름). 꺼져 있으면 빈 맵"
  value       = { for name, ep in aws_vpc_endpoint.interface : name => ep.id }
}
