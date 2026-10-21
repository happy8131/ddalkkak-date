output "state_bucket_name" {
  description = "envs/prod 의 backend.hcl 에 넣을 상태 버킷 이름"
  value       = aws_s3_bucket.state.id
}

output "aws_region" {
  description = "상태 버킷이 있는 리전"
  value       = var.aws_region
}
