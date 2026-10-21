output "alarm_topic_arn" {
  description = "알람 SNS 토픽 ARN. Slack 등 다른 구독자를 붙일 때 쓴다"
  value       = aws_sns_topic.alarms.arn
}
