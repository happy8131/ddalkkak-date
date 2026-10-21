# GitHub Actions 가 장기 액세스 키 없이 AWS 에 접근하도록 OIDC 공급자와 배포용 IAM 역할을 만든다.
# 역할은 지정한 저장소의 GitHub Environment(기본 prod)에서 실행된 워크플로만 맡을 수 있고,
# 권한은 이미지 push 와 ECS 서비스 갱신에 필요한 것으로 한정한다.
# Terraform 변경(plan/apply)은 CI/CD 대상이 아니므로 이 역할에는 인프라 변경 권한이 없다.

locals {
  oidc_host = "token.actions.githubusercontent.com"
  # 환경(environment) 단위로 제한하면 GitHub 의 환경 보호 규칙(브랜치 제한, 승인)을 함께 쓸 수 있다
  subject = "repo:${var.github_repository}:environment:${var.github_environment}"
}

# 계정마다 같은 URL 의 OIDC 공급자는 하나만 만들 수 있다. 이미 있으면 create_oidc_provider = false 로 재사용한다
resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 1 : 0

  url            = "https://${local.oidc_host}"
  client_id_list = ["sts.amazonaws.com"]

  tags = merge(var.tags, { Name = "${var.name_prefix}-github-oidc" })
}

data "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 0 : 1

  url = "https://${local.oidc_host}"
}

locals {
  oidc_provider_arn = var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn
}

resource "aws_iam_role" "deploy" {
  name = "${var.name_prefix}-github-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = local.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_host}:aud" = "sts.amazonaws.com"
          "${local.oidc_host}:sub" = local.subject
        }
      }
    }]
  })

  tags = merge(var.tags, { Name = "${var.name_prefix}-github-deploy" })
}

resource "aws_iam_role_policy" "deploy" {
  name = "deploy"
  role = aws_iam_role.deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "EcrLogin"
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        Sid    = "EcrPush"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage",
        ]
        Resource = values(var.ecr_repository_arns)
      },
      {
        # 태스크 정의 조회/등록은 리소스 수준 권한을 지원하지 않는다
        Sid    = "TaskDefinition"
        Effect = "Allow"
        Action = [
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition",
        ]
        Resource = "*"
      },
      {
        Sid    = "UpdateServices"
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",
          "ecs:UpdateService",
        ]
        Resource = values(var.ecs_service_arns)
      },
      {
        # 새 태스크 정의에 기존 실행/태스크 역할을 지정할 때만 ECS 에 넘길 수 있게 한다
        Sid      = "PassTaskRoles"
        Effect   = "Allow"
        Action   = "iam:PassRole"
        Resource = var.pass_role_arns
        Condition = {
          StringEquals = { "iam:PassedToService" = "ecs-tasks.amazonaws.com" }
        }
      },
    ]
  })
}
