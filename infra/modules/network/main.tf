# VPC 한 개에 서브넷 3계층을 둔다.
#   public      : ALB, NAT 게이트웨이
#   private_app : ECS Fargate 태스크 (인터넷 접근은 NAT 경유, S3 는 게이트웨이 엔드포인트)
#   private_db  : RDS (인터넷 경로 없음)

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, var.az_count)
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(var.tags, { Name = "${var.name_prefix}-vpc" })
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.tags, { Name = "${var.name_prefix}-igw" })
}

# --- 서브넷 ---

resource "aws_subnet" "public" {
  count = var.az_count

  vpc_id                  = aws_vpc.this.id
  availability_zone       = local.azs[count.index]
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  map_public_ip_on_launch = false

  tags = merge(var.tags, { Name = "${var.name_prefix}-public-${local.azs[count.index]}" })
}

resource "aws_subnet" "private_app" {
  count = var.az_count

  vpc_id            = aws_vpc.this.id
  availability_zone = local.azs[count.index]
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 10 + count.index)

  tags = merge(var.tags, { Name = "${var.name_prefix}-private-app-${local.azs[count.index]}" })
}

resource "aws_subnet" "private_db" {
  count = var.az_count

  vpc_id            = aws_vpc.this.id
  availability_zone = local.azs[count.index]
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 20 + count.index)

  tags = merge(var.tags, { Name = "${var.name_prefix}-private-db-${local.azs[count.index]}" })
}

# --- NAT ---
# 기본은 비용 절감을 위해 하나만 둔다. 이 경우 NAT 가 있는 AZ 에 장애가 나면 모든 private 아웃바운드가 끊긴다.
# nat_per_az = true 면 AZ 마다 NAT 를 두어 AZ 장애가 다른 AZ 로 번지지 않게 한다 (NAT 비용이 AZ 수만큼 늘어난다)

# enable_nat_gateway = false 면 NAT 를 만들지 않는다. 이때 ECS 태스크는 퍼블릭 서브넷에서
# 공인 IP 로 직접 인터넷에 나가야 한다 (ecs 모듈의 assign_public_ip). 비용 절감용 구성이다
locals {
  nat_count = var.enable_nat_gateway ? (var.nat_per_az ? var.az_count : 1) : 0
}

resource "aws_eip" "nat" {
  count = local.nat_count

  domain = "vpc"

  tags = merge(var.tags, { Name = "${var.name_prefix}-nat-eip-${count.index}" })

  depends_on = [aws_internet_gateway.this]
}

resource "aws_nat_gateway" "this" {
  count = local.nat_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(var.tags, { Name = "${var.name_prefix}-nat-${count.index}" })

  depends_on = [aws_internet_gateway.this]
}

# --- 라우팅 ---

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-public-rt" })
}

resource "aws_route_table_association" "public" {
  count = var.az_count

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# AZ 마다 라우트 테이블을 두고, nat_per_az 면 같은 AZ 의 NAT 로, 아니면 하나뿐인 NAT 로 보낸다
resource "aws_route_table" "private_app" {
  count = var.az_count

  vpc_id = aws_vpc.this.id

  # NAT 가 없으면 기본 경로도 없다 (프라이빗 서브넷은 VPC 안에서만 통신한다)
  dynamic "route" {
    for_each = var.enable_nat_gateway ? [1] : []

    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = aws_nat_gateway.this[var.nat_per_az ? count.index : 0].id
    }
  }

  tags = merge(var.tags, { Name = "${var.name_prefix}-private-app-rt-${local.azs[count.index]}" })
}

resource "aws_route_table_association" "private_app" {
  count = var.az_count

  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app[count.index].id
}

# DB 서브넷은 VPC 내부 라우트만 갖는다 (기본 local 라우트)
resource "aws_route_table" "private_db" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.tags, { Name = "${var.name_prefix}-private-db-rt" })
}

resource "aws_route_table_association" "private_db" {
  count = var.az_count

  subnet_id      = aws_subnet.private_db[count.index].id
  route_table_id = aws_route_table.private_db.id
}

# --- 보안 그룹 ---

resource "aws_security_group" "alb" {
  name        = "${var.name_prefix}-alb"
  description = "ALB: allow HTTP from the internet"
  vpc_id      = aws_vpc.this.id

  tags = merge(var.tags, { Name = "${var.name_prefix}-alb-sg" })
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP from internet"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

# HTTPS 리스너(ACM 인증서)를 붙일 때 켠다. 지금은 80 만 연다
resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  count = var.alb_https_enabled ? 1 : 0

  security_group_id = aws_security_group.alb.id
  description       = "HTTPS from internet"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "alb_all" {
  security_group_id = aws_security_group.alb.id
  description       = "ALB to targets"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_security_group" "app" {
  name        = "${var.name_prefix}-app"
  description = "ECS tasks: allow traffic from the ALB only"
  vpc_id      = aws_vpc.this.id

  tags = merge(var.tags, { Name = "${var.name_prefix}-app-sg" })
}

resource "aws_vpc_security_group_ingress_rule" "app_frontend" {
  security_group_id            = aws_security_group.app.id
  description                  = "frontend from ALB"
  ip_protocol                  = "tcp"
  from_port                    = var.frontend_port
  to_port                      = var.frontend_port
  referenced_security_group_id = aws_security_group.alb.id
}

resource "aws_vpc_security_group_ingress_rule" "app_backend" {
  security_group_id            = aws_security_group.app.id
  description                  = "backend from ALB"
  ip_protocol                  = "tcp"
  from_port                    = var.backend_port
  to_port                      = var.backend_port
  referenced_security_group_id = aws_security_group.alb.id
}

# ECR 이미지 pull, Secrets Manager, CloudWatch Logs 호출을 위해 아웃바운드를 연다
resource "aws_vpc_security_group_egress_rule" "app_all" {
  security_group_id = aws_security_group.app.id
  description       = "outbound via NAT"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_security_group" "rds" {
  name        = "${var.name_prefix}-rds"
  description = "RDS: allow PostgreSQL from ECS tasks only"
  vpc_id      = aws_vpc.this.id

  tags = merge(var.tags, { Name = "${var.name_prefix}-rds-sg" })
}

resource "aws_vpc_security_group_ingress_rule" "rds_postgres" {
  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL from ECS tasks"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.app.id
}

# --- VPC 엔드포인트 ---
# S3 게이트웨이 엔드포인트는 무료이고, ECR 이미지 레이어가 S3 에서 내려오므로 NAT 데이터 처리량을 줄여 준다.
# 인터페이스 엔드포인트(ECR, CloudWatch Logs, Secrets Manager)는 시간당 요금이 AZ 마다 붙어서 기본은 끄고,
# NAT 트래픽 비용이 더 커질 때 enable_interface_endpoints 로 켠다.

data "aws_region" "current" {}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${data.aws_region.current.region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = aws_route_table.private_app[*].id

  tags = merge(var.tags, { Name = "${var.name_prefix}-s3-endpoint" })
}

locals {
  interface_endpoint_services = var.enable_interface_endpoints ? toset(["ecr.api", "ecr.dkr", "logs", "secretsmanager"]) : toset([])
}

resource "aws_security_group" "endpoints" {
  count = var.enable_interface_endpoints ? 1 : 0

  name        = "${var.name_prefix}-endpoints"
  description = "VPC interface endpoints: allow HTTPS from ECS tasks only"
  vpc_id      = aws_vpc.this.id

  tags = merge(var.tags, { Name = "${var.name_prefix}-endpoints-sg" })
}

resource "aws_vpc_security_group_ingress_rule" "endpoints_https" {
  count = var.enable_interface_endpoints ? 1 : 0

  security_group_id            = aws_security_group.endpoints[0].id
  description                  = "HTTPS from ECS tasks"
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  referenced_security_group_id = aws_security_group.app.id
}

resource "aws_vpc_endpoint" "interface" {
  for_each = local.interface_endpoint_services

  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${data.aws_region.current.region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private_app[*].id
  security_group_ids  = [aws_security_group.endpoints[0].id]
  private_dns_enabled = true

  tags = merge(var.tags, { Name = "${var.name_prefix}-${each.key}-endpoint" })
}
