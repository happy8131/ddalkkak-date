# GitHub Actions 가 ECR push 와 ECS 배포를 할 때 쓰는 배포 전용 IAM 사용자를 만든다.
# 권한은 이미지 push 와 ECS 서비스 갱신에 필요한 것으로 한정한다.
# Terraform 변경(plan/apply)은 CI/CD 대상이 아니므로 이 사용자에게는 인프라 변경 권한이 없다.
#
# 액세스 키는 여기서 만들지 않는다. aws_iam_access_key 를 쓰면 시크릿 키가 terraform.tfstate 에 평문으로 남는다.
# apply 후 콘솔(IAM > 사용자 > 보안 자격 증명) 또는 CLI 로 직접 발급해 GitHub 시크릿에 등록한다.

resource "aws_iam_user" "deploy" {
  name = "${var.name_prefix}-github-deploy"

  tags = merge(var.tags, { Name = "${var.name_prefix}-github-deploy" })
}

resource "aws_iam_user_policy" "deploy" {
  name = "deploy"
  user = aws_iam_user.deploy.name

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
