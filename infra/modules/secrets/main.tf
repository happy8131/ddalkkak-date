# 외부 API 키 자리(SSM Parameter Store SecureString). 값은 Terraform 에 넣지 않는다.
# 파라미터만 placeholder 로 만들어 두고, 실제 값은 AWS 콘솔이나 CLI 로 별도 주입한다.
# lifecycle ignore_changes 덕분에 이후 terraform apply 가 주입한 값을 placeholder 로 되돌리지 않는다.
#
# DB 비밀번호는 여기가 아니라 RDS 가 Secrets Manager 에서 직접 관리한다 (rds 모듈).
# 태스크에는 아직 주입하지 않는다. 사용하는 쪽(예: 장소 수집 배치 태스크)을 만들 때
# secrets 항목에 ARN 을 연결하고 실행 역할에 ssm:GetParameters 권한을 추가한다.

locals {
  placeholder = "REPLACE_ME"
}

resource "aws_ssm_parameter" "this" {
  for_each = var.parameters

  name        = "/${var.name_prefix}/${each.key}"
  description = each.value
  type        = "SecureString"
  value       = local.placeholder

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}" })

  lifecycle {
    ignore_changes = [value]
  }
}
