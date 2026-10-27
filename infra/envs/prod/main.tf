provider "aws" {
  region = var.aws_region

  # 모든 리소스에 공통 태그를 자동으로 붙인다
  default_tags {
    tags = local.common_tags
  }
}

locals {
  name_prefix = "${var.project}-${var.env}"

  common_tags = {
    project = var.project
    env     = var.env
    owner   = var.owner
  }
}

module "network" {
  source = "../../modules/network"

  name_prefix        = local.name_prefix
  tags               = local.common_tags
  enable_nat_gateway = var.enable_nat_gateway
}

module "ecr" {
  source = "../../modules/ecr"

  name_prefix = local.name_prefix
  tags        = local.common_tags
}

module "rds" {
  source = "../../modules/rds"

  name_prefix           = local.name_prefix
  tags                  = local.common_tags
  subnet_ids            = module.network.private_db_subnet_ids
  security_group_id     = module.network.rds_security_group_id
  multi_az              = var.db_multi_az
  instance_class        = var.db_instance_class
  backup_retention_days = var.db_backup_retention_days
}

module "ecs" {
  source = "../../modules/ecs"

  name_prefix = local.name_prefix
  tags        = local.common_tags
  aws_region  = var.aws_region

  vpc_id                 = module.network.vpc_id
  public_subnet_ids      = module.network.public_subnet_ids
  private_app_subnet_ids = module.network.private_app_subnet_ids
  alb_security_group_id  = module.network.alb_security_group_id
  app_security_group_id  = module.network.app_security_group_id

  # NAT 가 없으면 태스크가 공인 IP 로 직접 나가야 한다
  assign_public_ip = !var.enable_nat_gateway

  repository_urls    = module.ecr.repository_urls
  repository_arns    = module.ecr.repository_arns
  frontend_image_tag = var.frontend_image_tag
  backend_image_tag  = var.backend_image_tag

  db_address    = module.rds.address
  db_port       = module.rds.port
  db_name       = module.rds.db_name
  db_secret_arn = module.rds.master_user_secret_arn
}

module "observability" {
  source = "../../modules/observability"

  name_prefix = local.name_prefix
  tags        = local.common_tags
  alarm_email = var.alarm_email

  alb_arn_suffix            = module.ecs.alb_arn_suffix
  target_group_arn_suffixes = module.ecs.target_group_arn_suffixes
  cluster_name              = module.ecs.cluster_name
  service_names             = module.ecs.service_names
  db_identifier             = module.rds.identifier
}

# 외부 API 키 자리 (값은 콘솔/CLI 로 별도 주입)
module "secrets" {
  source = "../../modules/secrets"

  name_prefix = local.name_prefix
  tags        = local.common_tags
}

# GitHub Actions 배포용 IAM 사용자 (액세스 키는 Terraform 밖에서 발급)
module "cicd" {
  source = "../../modules/cicd"

  name_prefix = local.name_prefix
  tags        = local.common_tags

  ecr_repository_arns = module.ecr.repository_arns
  ecs_service_arns    = module.ecs.service_arns
  pass_role_arns      = [module.ecs.task_execution_role_arn, module.ecs.task_role_arn]
}
