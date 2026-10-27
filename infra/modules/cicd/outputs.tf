output "deploy_user_name" {
  description = "GitHub Actions 배포용 IAM 사용자 이름. 이 사용자의 액세스 키를 발급해 GitHub Environment(prod)의 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 시크릿에 넣는다"
  value       = aws_iam_user.deploy.name
}

output "deploy_user_arn" {
  description = "GitHub Actions 배포용 IAM 사용자 ARN"
  value       = aws_iam_user.deploy.arn
}
