# frontend / backend 이미지 저장소.
# 배포 이미지는 git SHA 태그로 push 하므로 태그마다 내용이 하나로 고정되는 IMMUTABLE 을 기본으로 쓴다.
# 같은 태그를 다시 push 하면 실패하고, latest 같은 움직이는 태그는 쓰지 않는다 (배포 추적과 롤백을 위해).

locals {
  repositories = toset(["frontend", "backend"])
}

resource "aws_ecr_repository" "this" {
  for_each = local.repositories

  name                 = "${var.name_prefix}/${each.key}"
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}" })
}

# 이미지가 계속 쌓이지 않도록 정리한다.
#   1) 태그 없는 이미지(덮어써져서 고아가 된 레이어 등)는 untagged_expire_days 일 뒤 삭제
#   2) 나머지는 최근 keep_image_count 개만 보관 (tagStatus any 규칙은 우선순위가 가장 낮아야 한다)
resource "aws_ecr_lifecycle_policy" "this" {
  for_each = aws_ecr_repository.this

  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "태그 없는 이미지는 ${var.untagged_expire_days}일 뒤 삭제"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_expire_days
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "최근 ${var.keep_image_count}개 이미지만 보관"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.keep_image_count
        }
        action = { type = "expire" }
      },
    ]
  })
}
