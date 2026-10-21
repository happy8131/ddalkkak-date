# RDS PostgreSQL. 마스터 비밀번호는 RDS가 Secrets Manager에 직접 만들고 관리한다
# (manage_master_user_password). 비밀번호가 코드, tfvars, Terraform 변수에 나타나지 않는다.
# 다만 Terraform state 에는 secret ARN 만 남고 비밀번호 값은 남지 않는다.

resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-db"
  subnet_ids = var.subnet_ids

  tags = merge(var.tags, { Name = "${var.name_prefix}-db-subnet-group" })
}

resource "aws_db_instance" "this" {
  identifier = "${var.name_prefix}-postgres"

  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  db_name  = var.db_name
  username = var.master_username

  manage_master_user_password = true

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false
  multi_az               = var.multi_az

  backup_retention_period    = var.backup_retention_days
  auto_minor_version_upgrade = true

  # prod 데이터 보호: 실수로 삭제하거나 destroy 해도 마지막 스냅샷이 남도록 한다
  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.name_prefix}-postgres-final"

  tags = merge(var.tags, { Name = "${var.name_prefix}-postgres" })
}
