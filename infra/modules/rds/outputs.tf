output "identifier" {
  description = "DB 인스턴스 식별자 (CloudWatch 알람 차원)"
  value       = aws_db_instance.this.identifier
}

output "address" {
  description = "DB 엔드포인트 호스트 이름"
  value       = aws_db_instance.this.address
}

output "port" {
  description = "DB 포트"
  value       = aws_db_instance.this.port
}

output "db_name" {
  description = "데이터베이스 이름"
  value       = aws_db_instance.this.db_name
}

output "master_user_secret_arn" {
  description = "RDS가 관리하는 마스터 계정 Secret ARN (JSON 키: username, password)"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
