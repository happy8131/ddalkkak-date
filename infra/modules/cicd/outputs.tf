output "deploy_role_arn" {
  description = "GitHub Actions 가 맡을 배포 역할 ARN. GitHub Environment(prod)의 AWS_DEPLOY_ROLE_ARN 시크릿에 넣는다"
  value       = aws_iam_role.deploy.arn
}

output "oidc_provider_arn" {
  description = "GitHub OIDC 공급자 ARN"
  value       = local.oidc_provider_arn
}
