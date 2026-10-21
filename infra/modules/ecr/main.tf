# frontend / backend 이미지 저장소.
# CI(GitHub Actions)가 git SHA 태그로 push 하고, 최초 배포용으로 latest 도 쓸 수 있도록 태그는 변경 가능하게 둔다.

locals {
  repositories = toset(["frontend", "backend"])
}

resource "aws_ecr_repository" "this" {
  for_each = local.repositories

  name                 = "${var.name_prefix}/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}" })
}

# 이미지가 계속 쌓이지 않도록 최근 N개만 남긴다
resource "aws_ecr_lifecycle_policy" "this" {
  for_each = aws_ecr_repository.this

  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "최근 ${var.keep_image_count}개 이미지만 보관"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.keep_image_count
        }
        action = { type = "expire" }
      }
    ]
  })
}
