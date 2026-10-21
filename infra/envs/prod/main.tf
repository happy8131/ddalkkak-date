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

# 모듈은 리소스가 추가될 때 하나씩 연결한다. 지금은 구조만 잡혀 있다.
# module "network" {
#   source      = "../../modules/network"
#   name_prefix = local.name_prefix
#   tags        = local.common_tags
# }
