output "repository_urls" {
  description = "리포지토리 URL (키: frontend, backend)"
  value       = { for name, repo in aws_ecr_repository.this : name => repo.repository_url }
}

output "repository_arns" {
  description = "리포지토리 ARN (키: frontend, backend)"
  value       = { for name, repo in aws_ecr_repository.this : name => repo.arn }
}

output "frontend_repository_url" {
  description = "frontend 리포지토리 URL"
  value       = aws_ecr_repository.this["frontend"].repository_url
}

output "backend_repository_url" {
  description = "backend 리포지토리 URL"
  value       = aws_ecr_repository.this["backend"].repository_url
}
