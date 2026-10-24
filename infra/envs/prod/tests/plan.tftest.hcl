# AWS 자격 증명 없이 전체 구성을 검증하는 테스트 (mock provider 로 apply 를 흉내 낸다).
# 실행: cd infra/envs/prod && terraform init && terraform test
# 실제 AWS 계정에는 아무것도 만들지 않는다. 리소스 세부 설정은 각 모듈의 tests/ 에서 확인하고,
# 실제 plan 은 README 의 절차대로 따로 확인한다.

mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["us-east-1a", "us-east-1b", "us-east-1c"]
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

  mock_data "aws_region" {
    defaults = { region = "us-east-1" }
  }

  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }
}

# RDS 는 관리형 비밀번호를 쓰므로 Secret ARN 이 apply 후에 정해진다. 테스트에서는 값을 지정해 준다
override_resource {
  target = module.rds.aws_db_instance.this
  values = {
    identifier = "ddalkkak-date-prod-postgres"
    address    = "ddalkkak-date-prod-postgres.abcdefg.us-east-1.rds.amazonaws.com"
    port       = 5432
    db_name    = "ddalkkak"
    master_user_secret = [{
      secret_arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:rds!db-test-AbCdEf"
    }]
  }
}

# 로컬 terraform.tfvars 의 값(NAT 끔, 무료 플랜용 DB 사양 등)이 테스트에 새어 들어오지 않도록
# 같은 이름의 변수를 기본값으로 고정한다. 다른 값이 필요한 run 은 자기 variables 에서 덮어쓴다
variables {
  owner                    = "tester"
  enable_nat_gateway       = true
  db_instance_class        = "db.t3.medium"
  db_backup_retention_days = 7
  db_multi_az              = false
  alarm_email              = ""
}

run "wire_modules" {
  command = apply

  assert {
    condition     = var.aws_region == "us-east-1"
    error_message = "리전은 us-east-1 이어야 합니다."
  }

  assert {
    condition     = local.common_tags == { project = "ddalkkak-date", env = "prod", owner = "tester" }
    error_message = "공통 태그(project, env, owner)가 올바르지 않습니다."
  }

  assert {
    condition     = toset(keys(output.ecr_repository_urls)) == toset(["backend", "frontend"])
    error_message = "ECR 리포지토리는 frontend, backend 두 개여야 합니다."
  }

  assert {
    condition     = toset(keys(output.ecs_service_names)) == toset(["backend", "frontend"])
    error_message = "ECS 서비스는 frontend, backend 두 개여야 합니다."
  }

  assert {
    condition     = length(output.api_key_parameter_names) == 5
    error_message = "외부 API 키 파라미터 5개가 출력되어야 합니다."
  }

  assert {
    condition     = output.alb_dns_name != "" && output.alarm_topic_arn != ""
    error_message = "ALB 주소와 알람 토픽이 출력되어야 합니다."
  }
}

# 비용 절감 구성: NAT 를 끄면 prod 전체가 문제없이 연결된다
run "wire_modules_no_nat" {
  command = apply

  variables {
    enable_nat_gateway = false
  }

  assert {
    condition     = output.alb_dns_name != "" && length(output.ecs_service_names) == 2
    error_message = "NAT 를 꺼도 ALB 와 ECS 서비스가 구성되어야 합니다."
  }
}
