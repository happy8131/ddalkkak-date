terraform {
  required_version = ">= 1.10.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # 원격 상태(S3 백엔드)는 쓰지 않는다. 상태는 이 디렉터리의 terraform.tfstate(로컬)에 저장된다.
}
