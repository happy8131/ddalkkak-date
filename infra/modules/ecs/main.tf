# ECS Fargate 클러스터, frontend / backend 서비스, ALB, CloudWatch Logs.
# ALB 라우팅: /api/* -> backend, 그 외 -> frontend

locals {
  services = {
    frontend = {
      port              = var.frontend_port
      cpu               = var.frontend_cpu
      memory            = var.frontend_memory
      desired_count     = var.frontend_desired_count
      min_capacity      = var.frontend_min_capacity
      max_capacity      = var.frontend_max_capacity
      image             = "${var.repository_urls["frontend"]}:${var.frontend_image_tag}"
      health_check_path = "/"
      grace_seconds     = 60
    }
    backend = {
      port              = var.backend_port
      cpu               = var.backend_cpu
      memory            = var.backend_memory
      desired_count     = var.backend_desired_count
      min_capacity      = var.backend_min_capacity
      max_capacity      = var.backend_max_capacity
      image             = "${var.repository_urls["backend"]}:${var.backend_image_tag}"
      health_check_path = "/actuator/health"
      grace_seconds     = 180 # Spring Boot + Flyway 기동 시간을 감안한다
    }
  }
}

locals {
  # 서비스마다 환경 변수가 달라서 객체로 따로 둔다 (조건식은 양쪽 타입이 같아야 해서 쓰지 않는다)
  container_extras = {
    frontend = {
      environment = [
        { name = "PORT", value = tostring(var.frontend_port) },
        { name = "HOSTNAME", value = "0.0.0.0" },
      ]
      # node:slim 이미지에는 curl 이 없어서 Node 22 내장 fetch 로 확인한다
      healthCheck = {
        command     = ["CMD", "node", "-e", "fetch('http://127.0.0.1:${var.frontend_port}/').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 30
      }
    }
    backend = {
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "DB_URL", value = "jdbc:postgresql://${var.db_address}:${var.db_port}/${var.db_name}" },
        # HTTPS 도입 전이라 ALB 주소(http)를 허용 출처로 쓴다
        { name = "CORS_ALLOWED_ORIGINS", value = "http://${aws_lb.this.dns_name}" },
      ]
      # 비밀번호는 코드에 두지 않고 컨테이너 시작 시점에 Secrets Manager 에서 주입한다
      secrets = [
        { name = "DB_USERNAME", valueFrom = "${var.db_secret_arn}:username::" },
        { name = "DB_PASSWORD", valueFrom = "${var.db_secret_arn}:password::" },
      ]
      # eclipse-temurin 이미지에는 curl 이 있다. Spring Boot + Flyway 기동 시간을 startPeriod 로 기다린다
      healthCheck = {
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.backend_port}/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 120
      }
    }
  }
}

# --- 클러스터와 로그 ---

resource "aws_ecs_cluster" "this" {
  name = "${var.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-cluster" })
}

resource "aws_cloudwatch_log_group" "this" {
  for_each = local.services

  name              = "/ecs/${var.name_prefix}/${each.key}"
  retention_in_days = var.log_retention_days

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}-logs" })
}

# --- IAM ---

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# 실행 역할: ECR 이미지 pull, 로그 전송, 컨테이너 시작 시 비밀 값 주입
resource "aws_iam_role" "task_execution" {
  name               = "${var.name_prefix}-ecs-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json

  tags = var.tags
}

# AmazonECSTaskExecutionRolePolicy 는 로그/ECR 권한이 모든 리소스(*)에 걸려 있어서 쓰지 않고,
# 이 스택의 ECR 리포지토리, 로그 그룹, DB Secret 으로 범위를 좁힌 정책을 직접 둔다
resource "aws_iam_role_policy" "task_execution" {
  name = "task-execution"
  role = aws_iam_role.task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "EcrLogin"
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*" # 리소스 수준 권한을 지원하지 않는다
      },
      {
        Sid    = "EcrPull"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
        ]
        Resource = values(var.repository_arns)
      },
      {
        Sid      = "WriteLogs"
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = [for lg in aws_cloudwatch_log_group.this : "${lg.arn}:*"]
      },
      {
        Sid      = "ReadDbSecret"
        Effect   = "Allow"
        Action   = "secretsmanager:GetSecretValue"
        Resource = var.db_secret_arn
      },
    ]
  })
}

# 태스크 역할: 애플리케이션이 AWS API 를 호출할 때 쓴다. 지금은 권한이 없고 필요할 때 추가한다
resource "aws_iam_role" "task" {
  name               = "${var.name_prefix}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json

  tags = var.tags
}

# --- ALB ---

resource "aws_lb" "this" {
  name               = "${var.name_prefix}-alb"
  load_balancer_type = "application"
  internal           = false
  subnets            = var.public_subnet_ids
  security_groups    = [var.alb_security_group_id]

  drop_invalid_header_fields = true

  tags = merge(var.tags, { Name = "${var.name_prefix}-alb" })
}

resource "aws_lb_target_group" "this" {
  for_each = local.services

  # name_prefix 는 최대 6자이고, create_before_destroy 로 교체할 때 이름이 겹치지 않게 해 준다
  name_prefix          = substr(each.key, 0, 2)
  port                 = each.value.port
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = var.vpc_id
  deregistration_delay = 30

  health_check {
    path                = each.value.health_check_path
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}-tg" })

  lifecycle {
    create_before_destroy = true
  }
}

# 기본 동작은 frontend. HTTPS(ACM)는 초기 안정화 이후 별도 작업이라 80 포트만 연다
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this["frontend"].arn
  }
}

resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this["backend"].arn
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }
}

# --- 태스크 정의와 서비스 ---

resource "aws_ecs_task_definition" "this" {
  for_each = local.services

  family                   = "${var.name_prefix}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = var.cpu_architecture
  }

  container_definitions = jsonencode([
    merge(
      {
        name      = each.key
        image     = each.value.image
        essential = true

        portMappings = [{
          containerPort = each.value.port
          protocol      = "tcp"
        }]

        logConfiguration = {
          logDriver = "awslogs"
          options = {
            "awslogs-group"         = aws_cloudwatch_log_group.this[each.key].name
            "awslogs-region"        = var.aws_region
            "awslogs-stream-prefix" = each.key
          }
        }
      },
      local.container_extras[each.key]
    )
  ])

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}" })
}

resource "aws_ecs_service" "this" {
  for_each = local.services

  name                              = "${var.name_prefix}-${each.key}"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.this[each.key].arn
  desired_count                     = each.value.desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = each.value.grace_seconds

  network_configuration {
    # NAT 없이 쓰는 구성(assign_public_ip = true)에서는 퍼블릭 서브넷에서 공인 IP 로 직접 인터넷에 나간다.
    # 인바운드는 보안 그룹이 ALB 에서 오는 트래픽만 허용하므로 공인 IP 가 있어도 직접 접근은 막힌다
    subnets          = var.assign_public_ip ? var.public_subnet_ids : var.private_app_subnet_ids
    security_groups  = [var.app_security_group_id]
    assign_public_ip = var.assign_public_ip
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.this[each.key].arn
    container_name   = each.key
    container_port   = each.value.port
  }

  # 새 태스크가 헬스체크를 통과하지 못하면 이전 버전으로 되돌린다
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}" })

  # 이미지 배포는 CI/CD 가 태스크 정의를 갱신하는 방식이라 Terraform 이 되돌리지 않게 한다.
  # desired_count 는 Auto Scaling 이 조정하므로 처음 생성할 때의 값만 쓴다
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  depends_on = [aws_lb_listener.http]
}

# --- Application Auto Scaling (CPU 기반, 초기에는 최소/최대 값만 둔다) ---

resource "aws_appautoscaling_target" "this" {
  for_each = local.services

  service_namespace  = "ecs"
  scalable_dimension = "ecs:service:DesiredCount"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.this[each.key].name}"
  min_capacity       = each.value.min_capacity
  max_capacity       = each.value.max_capacity
}

resource "aws_appautoscaling_policy" "cpu" {
  for_each = local.services

  name               = "${var.name_prefix}-${each.key}-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.this[each.key].service_namespace
  scalable_dimension = aws_appautoscaling_target.this[each.key].scalable_dimension
  resource_id        = aws_appautoscaling_target.this[each.key].resource_id

  target_tracking_scaling_policy_configuration {
    target_value       = var.autoscaling_cpu_target
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
