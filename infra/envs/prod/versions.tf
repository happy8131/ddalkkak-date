terraform {
  required_version = ">= 1.10.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # 버킷 이름은 계정마다 달라서 코드에 넣지 않고 init 때 -backend-config=backend.hcl 로 넘긴다.
  # 상태 잠금은 DynamoDB 없이 S3 네이티브 락(use_lockfile)을 쓴다
  backend "s3" {
    key          = "envs/prod/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}
