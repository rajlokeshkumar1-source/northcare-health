locals {
  common_tags = {
    Project     = var.identifier
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = "northcare-health"
  }
}

# ─── Random Password ─────────────────────────────────────────────────────────

resource "random_password" "db_password" {
  length           = 24
  special          = true
  override_special = "!#$%&()-_=+[]<>?"
}

# ─── Secrets Manager ─────────────────────────────────────────────────────────

resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "${var.identifier}-db-credentials"
  description             = "PostgreSQL credentials for ${var.identifier} (managed by Terraform)"
  recovery_window_in_days = 7

  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = var.username
    password = random_password.db_password.result
    host     = aws_db_instance.main.address
    port     = aws_db_instance.main.port
    dbname   = var.db_name
    engine   = "postgres"
  })
}

# ─── DB Subnet Group ─────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "main" {
  name       = "${var.identifier}-subnet-group"
  subnet_ids = var.subnet_ids
  description = "Private subnet group for ${var.identifier}"

  tags = merge(local.common_tags, {
    Name = "${var.identifier}-subnet-group"
  })
}

# ─── Security Group ──────────────────────────────────────────────────────────

resource "aws_security_group" "rds" {
  name        = "${var.identifier}-rds-sg"
  description = "Allow PostgreSQL access from EKS worker nodes"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from EKS managed node group"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.eks_node_sg_id]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${var.identifier}-rds-sg"
  })
}

# ─── Parameter Group ─────────────────────────────────────────────────────────

resource "aws_db_parameter_group" "main" {
  name   = "${var.identifier}-pg15"
  family = "postgres15"
  description = "Custom parameter group for ${var.identifier}"

  parameter {
    name  = "log_connections"
    value = "1"
  }

  parameter {
    name  = "log_disconnections"
    value = "1"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "5000" # log queries slower than 5 s
  }

  tags = local.common_tags
}

# ─── RDS Instance ─────────────────────────────────────────────────────────────

resource "aws_db_instance" "main" {
  identifier        = var.identifier
  engine            = "postgres"
  engine_version    = "15.4"
  instance_class    = var.instance_class
  allocated_storage = 20
  storage_type      = "gp2"
  storage_encrypted = true # HIPAA-aligned: encrypt at rest

  db_name  = var.db_name
  username = var.username
  password = random_password.db_password.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.main.name

  multi_az                    = false        # single-AZ keeps costs low for dev
  publicly_accessible         = false
  backup_retention_period     = 7
  backup_window               = "03:00-04:00"
  maintenance_window          = "sun:04:00-sun:05:00"
  auto_minor_version_upgrade  = true
  deletion_protection         = false        # simulator: allow teardown
  skip_final_snapshot         = true

  tags = merge(local.common_tags, {
    Name = var.identifier
  })
}
