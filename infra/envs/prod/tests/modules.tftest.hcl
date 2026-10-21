# 모듈 내부 리소스의 세부 설정을 검증하는 테스트 (mock provider, AWS 호출 없음).
# 실행: cd infra/envs/prod && terraform init && terraform test

mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["us-east-1a", "us-east-1b", "us-east-1c"]
    }
  }

  mock_data "aws_region" {
    defaults = { region = "us-east-1" }
  }

  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }

  # provider 가 ARN 형식을 검사하므로 mock 이 만드는 ARN 은 올바른 형식으로 지정한다
  mock_resource "aws_sns_topic" {
    defaults = { arn = "arn:aws:sns:us-east-1:123456789012:mock-topic" }
  }

  mock_resource "aws_iam_role" {
    defaults = { arn = "arn:aws:iam::123456789012:role/mock-role" }
  }

  mock_resource "aws_lb" {
    defaults = {
      arn        = "arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/mock-alb/0123456789abcdef"
      arn_suffix = "app/mock-alb/0123456789abcdef"
      dns_name   = "mock-alb-123456789.us-east-1.elb.amazonaws.com"
    }
  }

  mock_resource "aws_lb_target_group" {
    defaults = {
      arn        = "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/mock-tg/0123456789abcdef"
      arn_suffix = "targetgroup/mock-tg/0123456789abcdef"
    }
  }

  mock_resource "aws_lb_listener" {
    defaults = { arn = "arn:aws:elasticloadbalancing:us-east-1:123456789012:listener/app/mock-alb/0123456789abcdef/0123456789abcdef" }
  }
}

variables {
  owner = "tester"
}

# --- network ---

run "network" {
  command = apply

  module {
    source = "../../modules/network"
  }

  variables {
    name_prefix = "test"
  }

  assert {
    condition     = length(aws_subnet.public) == 2 && length(aws_subnet.private_app) == 2 && length(aws_subnet.private_db) == 2
    error_message = "서브넷은 계층(public, private_app, private_db)마다 2개 AZ 에 있어야 합니다."
  }

  # RDS 는 ECS 태스크 보안 그룹에서 오는 5432 만 허용한다
  assert {
    condition     = aws_vpc_security_group_ingress_rule.rds_postgres.from_port == 5432 && aws_vpc_security_group_ingress_rule.rds_postgres.cidr_ipv4 == null
    error_message = "RDS 는 보안 그룹 참조로만 접근을 허용해야 합니다."
  }

  # ECS 태스크는 ALB 보안 그룹에서 오는 트래픽만 받는다 (인터넷 CIDR 직접 허용 금지)
  assert {
    condition     = aws_vpc_security_group_ingress_rule.app_frontend.cidr_ipv4 == null && aws_vpc_security_group_ingress_rule.app_backend.cidr_ipv4 == null
    error_message = "ECS 태스크 보안 그룹이 인터넷에 열려 있습니다."
  }

  # 기본값: NAT 1개, HTTP 만 공개, S3 게이트웨이 엔드포인트만 (인터페이스 엔드포인트는 비용 때문에 끔)
  assert {
    condition     = length(aws_nat_gateway.this) == 1 && length(aws_route_table.private_app) == 2
    error_message = "기본값은 NAT 1개이고 private_app 라우트 테이블은 AZ 마다 하나여야 합니다."
  }

  assert {
    condition     = length(aws_vpc_security_group_ingress_rule.alb_https) == 0
    error_message = "기본값에서는 ALB 443 을 열지 않아야 합니다."
  }

  assert {
    condition     = aws_vpc_endpoint.s3.vpc_endpoint_type == "Gateway" && length(aws_vpc_endpoint.interface) == 0
    error_message = "기본값은 S3 게이트웨이 엔드포인트만 있어야 합니다."
  }

  # DB 서브넷에는 인터넷 경로가 없다
  assert {
    condition     = length(aws_route_table.private_db.route) == 0
    error_message = "DB 라우트 테이블에 외부 경로가 있으면 안 됩니다."
  }
}

# AZ 별 NAT, HTTPS, 인터페이스 엔드포인트를 모두 켠 구성
run "network_high_availability" {
  command = apply

  module {
    source = "../../modules/network"
  }

  variables {
    name_prefix                = "test"
    nat_per_az                 = true
    alb_https_enabled          = true
    enable_interface_endpoints = true
  }

  assert {
    condition     = length(aws_nat_gateway.this) == 2 && length(output.nat_public_ips) == 2
    error_message = "nat_per_az 면 AZ 마다 NAT 가 있어야 합니다."
  }

  assert {
    condition     = length(aws_vpc_security_group_ingress_rule.alb_https) == 1
    error_message = "alb_https_enabled 면 ALB 443 이 열려야 합니다."
  }

  assert {
    condition     = toset(keys(aws_vpc_endpoint.interface)) == toset(["ecr.api", "ecr.dkr", "logs", "secretsmanager"])
    error_message = "ECR(api, dkr), CloudWatch Logs, Secrets Manager 인터페이스 엔드포인트가 있어야 합니다."
  }

  assert {
    condition     = alltrue([for e in aws_vpc_endpoint.interface : e.private_dns_enabled])
    error_message = "인터페이스 엔드포인트는 프라이빗 DNS 를 써야 합니다."
  }
}

# --- ecr ---

run "ecr" {
  command = apply

  module {
    source = "../../modules/ecr"
  }

  variables {
    name_prefix = "test"
  }

  assert {
    condition     = toset(keys(aws_ecr_repository.this)) == toset(["frontend", "backend"]) && aws_ecr_repository.this["frontend"].name == "test/frontend" && aws_ecr_repository.this["backend"].name == "test/backend"
    error_message = "frontend, backend 리포지토리가 <접두사>/<이름> 으로 있어야 합니다."
  }

  assert {
    condition     = alltrue([for r in aws_ecr_repository.this : r.image_scanning_configuration[0].scan_on_push])
    error_message = "push 시 이미지 스캔이 켜져 있어야 합니다."
  }

  # git SHA 태그 전략: 태그는 변경 불가
  assert {
    condition     = alltrue([for r in aws_ecr_repository.this : r.image_tag_mutability == "IMMUTABLE"])
    error_message = "기본값은 태그 변경 불가(IMMUTABLE)여야 합니다."
  }

  assert {
    condition     = alltrue([for r in aws_ecr_repository.this : r.encryption_configuration[0].encryption_type == "AES256"])
    error_message = "리포지토리는 암호화되어야 합니다."
  }

  # 수명 주기: 태그 없는 이미지 정리 -> 최근 30개 보관 (any 규칙의 우선순위가 가장 낮아야 한다)
  assert {
    condition     = jsondecode(aws_ecr_lifecycle_policy.this["backend"].policy).rules[0].selection.tagStatus == "untagged" && jsondecode(aws_ecr_lifecycle_policy.this["backend"].policy).rules[0].selection.countNumber == 7
    error_message = "태그 없는 이미지는 7일 뒤 삭제해야 합니다."
  }

  assert {
    condition     = jsondecode(aws_ecr_lifecycle_policy.this["backend"].policy).rules[1].selection.tagStatus == "any" && jsondecode(aws_ecr_lifecycle_policy.this["backend"].policy).rules[1].selection.countNumber == 30 && jsondecode(aws_ecr_lifecycle_policy.this["backend"].policy).rules[1].rulePriority > jsondecode(aws_ecr_lifecycle_policy.this["backend"].policy).rules[0].rulePriority
    error_message = "최근 30개만 보관하는 규칙이 마지막 우선순위로 있어야 합니다."
  }

  assert {
    condition     = output.frontend_repository_url != "" && output.backend_repository_url != "" && length(output.repository_urls) == 2
    error_message = "repository_url 이 출력되어야 합니다."
  }
}

# 필요하면 태그 변경을 허용할 수 있다
run "ecr_mutable" {
  command = apply

  module {
    source = "../../modules/ecr"
  }

  variables {
    name_prefix          = "test"
    image_tag_mutability = "MUTABLE"
  }

  assert {
    condition     = alltrue([for r in aws_ecr_repository.this : r.image_tag_mutability == "MUTABLE"])
    error_message = "image_tag_mutability 로 MUTABLE 을 선택할 수 있어야 합니다."
  }
}

# --- rds ---

run "rds" {
  command = apply

  module {
    source = "../../modules/rds"
  }

  variables {
    name_prefix       = "test"
    subnet_ids        = ["subnet-aaaa1111", "subnet-bbbb2222"]
    security_group_id = "sg-12345678"
  }

  override_resource {
    target = aws_db_instance.this
    values = {
      identifier = "test-postgres"
      address    = "test-postgres.abcdefg.us-east-1.rds.amazonaws.com"
      port       = 5432
      db_name    = "ddalkkak"
      master_user_secret = [{
        secret_arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:rds!db-test-AbCdEf"
      }]
    }
  }

  assert {
    condition     = aws_db_instance.this.publicly_accessible == false
    error_message = "RDS 는 외부에 공개되면 안 됩니다."
  }

  assert {
    condition     = aws_db_instance.this.storage_encrypted == true && aws_db_instance.this.manage_master_user_password == true
    error_message = "RDS 는 암호화와 관리형 비밀번호를 써야 합니다."
  }

  assert {
    condition     = aws_db_instance.this.password == null
    error_message = "DB 비밀번호가 코드나 변수로 들어가면 안 됩니다."
  }

  assert {
    condition     = aws_db_instance.this.deletion_protection == true && aws_db_instance.this.skip_final_snapshot == false
    error_message = "삭제 보호와 최종 스냅샷이 켜져 있어야 합니다."
  }

  assert {
    condition     = aws_db_instance.this.engine == "postgres" && aws_db_instance.this.instance_class == "db.t3.medium"
    error_message = "PRD 기준 PostgreSQL db.t3.medium 이어야 합니다."
  }
}

# Multi-AZ 와 인스턴스 크기는 변수로 바꿀 수 있다
run "rds_multi_az" {
  command = apply

  module {
    source = "../../modules/rds"
  }

  variables {
    name_prefix       = "test"
    subnet_ids        = ["subnet-aaaa1111", "subnet-bbbb2222"]
    security_group_id = "sg-12345678"
    multi_az          = true
    instance_class    = "db.t3.large"
  }

  override_resource {
    target = aws_db_instance.this
    values = {
      identifier = "test-postgres"
      address    = "test-postgres.abcdefg.us-east-1.rds.amazonaws.com"
      port       = 5432
      db_name    = "ddalkkak"
      master_user_secret = [{
        secret_arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:rds!db-test-AbCdEf"
      }]
    }
  }

  assert {
    condition     = aws_db_instance.this.multi_az == true && aws_db_instance.this.instance_class == "db.t3.large"
    error_message = "multi_az 와 instance_class 변수가 반영되어야 합니다."
  }

  assert {
    condition     = aws_db_instance.this.deletion_protection == true && aws_db_instance.this.publicly_accessible == false
    error_message = "Multi-AZ 로 바꿔도 삭제 보호와 비공개는 유지되어야 합니다."
  }
}

# --- ecs ---

run "ecs" {
  command = apply

  module {
    source = "../../modules/ecs"
  }

  variables {
    name_prefix            = "test"
    aws_region             = "us-east-1"
    vpc_id                 = "vpc-12345678"
    public_subnet_ids      = ["subnet-aaaa1111", "subnet-bbbb2222"]
    private_app_subnet_ids = ["subnet-cccc3333", "subnet-dddd4444"]
    alb_security_group_id  = "sg-11111111"
    app_security_group_id  = "sg-22222222"
    repository_urls = {
      frontend = "123456789012.dkr.ecr.us-east-1.amazonaws.com/test/frontend"
      backend  = "123456789012.dkr.ecr.us-east-1.amazonaws.com/test/backend"
    }
    repository_arns = {
      frontend = "arn:aws:ecr:us-east-1:123456789012:repository/test/frontend"
      backend  = "arn:aws:ecr:us-east-1:123456789012:repository/test/backend"
    }
    db_address    = "test-postgres.abcdefg.us-east-1.rds.amazonaws.com"
    db_port       = 5432
    db_name       = "ddalkkak"
    db_secret_arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:rds!db-test-AbCdEf"
  }

  # ALB 라우팅: /api/* -> backend, 그 외(기본) -> frontend
  assert {
    condition     = anytrue([for c in aws_lb_listener_rule.api.condition : anytrue([for p in c.path_pattern : contains(p.values, "/api/*")])])
    error_message = "/api/* 경로 규칙이 없습니다."
  }

  assert {
    condition     = aws_lb_listener.http.port == 80 && aws_lb_listener.http.default_action[0].type == "forward"
    error_message = "ALB 80 포트 리스너가 frontend 로 전달해야 합니다."
  }

  # 이미지는 ECR 리포지토리에서 가져온다
  assert {
    condition     = strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "dkr.ecr.us-east-1.amazonaws.com/test/backend:")
    error_message = "backend 이미지가 ECR 에서 와야 합니다."
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.this["frontend"].container_definitions, "dkr.ecr.us-east-1.amazonaws.com/test/frontend:")
    error_message = "frontend 이미지가 ECR 에서 와야 합니다."
  }

  # 비밀 값: DB 계정은 평문 환경 변수가 아니라 Secrets Manager 로 주입한다
  assert {
    condition     = strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "\"name\":\"DB_PASSWORD\",\"valueFrom\"")
    error_message = "DB_PASSWORD 는 Secrets Manager 에서 주입해야 합니다."
  }

  assert {
    condition     = !strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "\"name\":\"DB_PASSWORD\",\"value\"")
    error_message = "DB_PASSWORD 가 평문 환경 변수로 들어가면 안 됩니다."
  }

  # Fargate, 퍼블릭 IP 없음, 배포 실패 시 롤백
  assert {
    condition     = alltrue([for s in aws_ecs_service.this : s.launch_type == "FARGATE" && s.network_configuration[0].assign_public_ip == false])
    error_message = "ECS 서비스는 퍼블릭 IP 없는 Fargate 여야 합니다."
  }

  assert {
    condition     = alltrue([for s in aws_ecs_service.this : s.deployment_circuit_breaker[0].rollback])
    error_message = "배포 실패 시 롤백(circuit breaker)이 켜져 있어야 합니다."
  }

  # 로그: 서비스마다 CloudWatch 로그 그룹, awslogs 드라이버
  assert {
    condition     = length(aws_cloudwatch_log_group.this) == 2
    error_message = "서비스마다 CloudWatch 로그 그룹이 있어야 합니다."
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "\"logDriver\":\"awslogs\"")
    error_message = "컨테이너 로그가 CloudWatch Logs 로 가야 합니다."
  }

  # Container Insights, 포트, 컨테이너 헬스체크
  assert {
    condition     = anytrue([for s in aws_ecs_cluster.this.setting : s.name == "containerInsights" && s.value == "enabled"])
    error_message = "Container Insights 가 켜져 있어야 합니다."
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "\"containerPort\":8080") && strcontains(aws_ecs_task_definition.this["frontend"].container_definitions, "\"containerPort\":3000")
    error_message = "컨테이너 포트가 backend 8080, frontend 3000 이어야 합니다."
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "actuator/health") && strcontains(aws_ecs_task_definition.this["backend"].container_definitions, "\"healthCheck\"")
    error_message = "backend 컨테이너 헬스체크가 /actuator/health 를 확인해야 합니다."
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.this["frontend"].container_definitions, "\"healthCheck\"") && !strcontains(aws_ecs_task_definition.this["frontend"].container_definitions, "curl")
    error_message = "frontend 컨테이너 헬스체크가 있어야 하고, curl 이 없는 이미지라 curl 을 쓰면 안 됩니다."
  }

  # ALB 타겟 그룹 헬스체크 경로
  assert {
    condition     = aws_lb_target_group.this["backend"].health_check[0].path == "/actuator/health" && aws_lb_target_group.this["frontend"].health_check[0].path == "/"
    error_message = "타겟 그룹 헬스체크 경로가 올바르지 않습니다."
  }

  # 실행 역할 최소 권한: 와일드카드 액션 없음, 관리형 광범위 정책 미사용, ECR pull 은 지정한 리포지토리로 한정
  assert {
    condition = alltrue([
      for st in jsondecode(aws_iam_role_policy.task_execution.policy).Statement :
      !anytrue([for a in flatten([st.Action]) : a == "*" || endswith(a, ":*")])
    ])
    error_message = "태스크 실행 역할에 와일드카드 액션이 있으면 안 됩니다."
  }

  assert {
    condition = anytrue([
      for st in jsondecode(aws_iam_role_policy.task_execution.policy).Statement :
      st.Sid == "EcrPull" && length(st.Resource) == 2 && alltrue([for r in st.Resource : startswith(r, "arn:aws:ecr:")])
    ])
    error_message = "ECR pull 권한은 frontend, backend 리포지토리로 한정되어야 합니다."
  }

  assert {
    condition = anytrue([
      for st in jsondecode(aws_iam_role_policy.task_execution.policy).Statement :
      st.Sid == "ReadDbSecret" && st.Resource == "arn:aws:secretsmanager:us-east-1:123456789012:secret:rds!db-test-AbCdEf"
    ])
    error_message = "Secrets 읽기 권한은 DB Secret 하나로 한정되어야 합니다."
  }

  # Auto Scaling: 서비스마다 CPU 목표 추적, 최소 1 / 최대 2
  assert {
    condition     = alltrue([for t in aws_appautoscaling_target.this : t.min_capacity == 1 && t.max_capacity == 2])
    error_message = "Auto Scaling 은 서비스마다 최소 1, 최대 2 여야 합니다."
  }

  assert {
    condition     = alltrue([for p in aws_appautoscaling_policy.cpu : p.target_tracking_scaling_policy_configuration[0].target_value == 70 && p.target_tracking_scaling_policy_configuration[0].predefined_metric_specification[0].predefined_metric_type == "ECSServiceAverageCPUUtilization"])
    error_message = "CPU 70% 목표 추적 정책이어야 합니다."
  }
}

# --- secrets ---

run "secrets" {
  command = apply

  module {
    source = "../../modules/secrets"
  }

  variables {
    name_prefix = "test"
  }

  # 카카오, LLM, Anthropic, 네이버 키 자리가 SecureString 으로 만들어진다
  assert {
    condition     = toset(keys(aws_ssm_parameter.this)) == toset(["kakao-rest-api-key", "llm-api-key", "anthropic-api-key", "naver-client-id", "naver-client-secret"])
    error_message = "외부 API 키 자리 5개가 있어야 합니다."
  }

  assert {
    condition     = alltrue([for p in aws_ssm_parameter.this : p.type == "SecureString"])
    error_message = "API 키 파라미터는 모두 SecureString 이어야 합니다."
  }

  assert {
    condition     = alltrue([for k, p in aws_ssm_parameter.this : p.name == "/test/${k}"])
    error_message = "파라미터 이름은 /<접두사>/<키> 형식이어야 합니다."
  }

  # 코드에는 placeholder 만 있고 실제 값은 없다
  assert {
    condition     = alltrue([for p in aws_ssm_parameter.this : p.value == "REPLACE_ME"])
    error_message = "코드에는 placeholder 값만 있어야 합니다."
  }

  assert {
    condition     = length(output.parameter_arns) == 5
    error_message = "파라미터 ARN 이 출력되어야 합니다."
  }
}

# --- observability ---

run "observability" {
  command = apply

  module {
    source = "../../modules/observability"
  }

  variables {
    name_prefix    = "test"
    alb_arn_suffix = "app/mock-alb/0123456789abcdef"
    target_group_arn_suffixes = {
      frontend = "targetgroup/fe/0123456789abcdef"
      backend  = "targetgroup/be/0123456789abcdef"
    }
    cluster_name = "test-cluster"
    service_names = {
      frontend = "test-frontend"
      backend  = "test-backend"
    }
    db_identifier = "test-postgres"
  }

  assert {
    condition     = aws_cloudwatch_metric_alarm.alb_5xx_rate.threshold == 5
    error_message = "ALB 5xx 비율 알람 임계치는 5% 여야 합니다."
  }

  assert {
    condition     = length(aws_cloudwatch_metric_alarm.ecs) == 4 && alltrue([for a in aws_cloudwatch_metric_alarm.ecs : a.threshold == 80])
    error_message = "ECS 서비스 2개 x (CPU, 메모리) = 알람 4개가 80% 임계치로 있어야 합니다."
  }

  assert {
    condition     = length(aws_cloudwatch_metric_alarm.unhealthy_hosts) == 2
    error_message = "타겟 그룹마다 헬스체크 실패 알람이 있어야 합니다."
  }

  # 이메일이 없으면 구독을 만들지 않는다
  assert {
    condition     = length(aws_sns_topic_subscription.email) == 0
    error_message = "alarm_email 이 비어 있으면 SNS 구독을 만들지 않아야 합니다."
  }
}
