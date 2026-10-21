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

  name_prefix = local.name_prefix
  tags        = local.common_tags
}

module "ecr" {
  source = "../../modules/ecr"

  name_prefix = local.name_prefix
  tags        = local.common_tags
}

module "rds" {
  source = "../../modules/rds"

  name_prefix       = local.name_prefix
  tags              = local.common_tags
  subnet_ids        = module.network.private_db_subnet_ids
  security_group_id = module.network.rds_security_group_id
  multi_az          = var.db_multi_az
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
