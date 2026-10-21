# 기본 CloudWatch 알람과 알림 SNS 토픽.
# 로그 수집(CloudWatch Logs)은 ecs 모듈의 로그 그룹이 맡고, 여기서는 알람만 만든다.
# 알람 -> SNS -> (이메일). Slack 연동은 이후 작업에서 같은 토픽에 붙인다.

resource "aws_sns_topic" "alarms" {
  name = "${var.name_prefix}-alarms"

  tags = merge(var.tags, { Name = "${var.name_prefix}-alarms" })
}

# 이메일을 지정했을 때만 구독을 만든다. 구독자가 메일의 확인 링크를 눌러야 활성화된다
resource "aws_sns_topic_subscription" "email" {
  count = var.alarm_email == "" ? 0 : 1

  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# --- ALB ---

# 요청 대비 5xx 비율(ALB 가 낸 5xx + 대상이 낸 5xx)이 임계치를 넘으면 알린다
resource "aws_cloudwatch_metric_alarm" "alb_5xx_rate" {
  alarm_name          = "${var.name_prefix}-alb-5xx-rate"
  alarm_description   = "ALB 5xx 비율이 ${var.alb_5xx_rate_threshold}%를 넘었습니다"
  comparison_operator = "GreaterThanThreshold"
  threshold           = var.alb_5xx_rate_threshold
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  metric_query {
    id          = "error_rate"
    expression  = "100 * (elb_5xx + target_5xx) / requests"
    label       = "5xx rate (%)"
    return_data = true
  }

  metric_query {
    id = "elb_5xx"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "HTTPCode_ELB_5XX_Count"
      period      = 300
      stat        = "Sum"
      dimensions  = { LoadBalancer = var.alb_arn_suffix }
    }
  }

  metric_query {
    id = "target_5xx"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "HTTPCode_Target_5XX_Count"
      period      = 300
      stat        = "Sum"
      dimensions  = { LoadBalancer = var.alb_arn_suffix }
    }
  }

  metric_query {
    id = "requests"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "RequestCount"
      period      = 300
      stat        = "Sum"
      dimensions  = { LoadBalancer = var.alb_arn_suffix }
    }
  }

  tags = var.tags
}

# 타겟 그룹의 비정상 대상 수. 헬스체크 실패가 이어지면 알린다
resource "aws_cloudwatch_metric_alarm" "unhealthy_hosts" {
  for_each = var.target_group_arn_suffixes

  alarm_name          = "${var.name_prefix}-${each.key}-unhealthy-hosts"
  alarm_description   = "${each.key} 타겟 그룹에 헬스체크를 통과하지 못하는 대상이 있습니다"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "UnHealthyHostCount"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  datapoints_to_alarm = 3
  comparison_operator = "GreaterThanThreshold"
  threshold           = 0
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = each.value
  }

  tags = var.tags
}

# --- ECS: 서비스별 CPU / 메모리 ---

locals {
  ecs_alarms = {
    for pair in setproduct(keys(var.service_names), ["CPUUtilization", "MemoryUtilization"]) :
    "${pair[0]}-${pair[1]}" => { service = pair[0], metric = pair[1] }
  }
}

resource "aws_cloudwatch_metric_alarm" "ecs" {
  for_each = local.ecs_alarms

  alarm_name          = "${var.name_prefix}-${each.value.service}-${lower(each.value.metric)}"
  alarm_description   = "${each.value.service} 서비스의 ${each.value.metric}이 ${var.ecs_utilization_threshold}%를 넘었습니다"
  namespace           = "AWS/ECS"
  metric_name         = each.value.metric
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  comparison_operator = "GreaterThanThreshold"
  threshold           = var.ecs_utilization_threshold
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  dimensions = {
    ClusterName = var.cluster_name
    ServiceName = var.service_names[each.value.service]
  }

  tags = var.tags
}

# --- RDS ---

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${var.name_prefix}-rds-cpuutilization"
  alarm_description   = "RDS CPU 사용률이 ${var.rds_cpu_threshold}%를 넘었습니다"
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  comparison_operator = "GreaterThanThreshold"
  threshold           = var.rds_cpu_threshold
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  dimensions = {
    DBInstanceIdentifier = var.db_identifier
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "rds_connections" {
  alarm_name          = "${var.name_prefix}-rds-connections"
  alarm_description   = "RDS 연결 수가 ${var.rds_connections_threshold}개를 넘었습니다"
  namespace           = "AWS/RDS"
  metric_name         = "DatabaseConnections"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  comparison_operator = "GreaterThanThreshold"
  threshold           = var.rds_connections_threshold
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  dimensions = {
    DBInstanceIdentifier = var.db_identifier
  }

  tags = var.tags
}
