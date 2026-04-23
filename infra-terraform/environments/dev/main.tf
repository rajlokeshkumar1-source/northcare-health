locals {
  db_identifier = "${var.project_name}-${var.environment}-db"
}

# ─── VPC ─────────────────────────────────────────────────────────────────────

module "vpc" {
  source = "../../modules/vpc-standard"

  project_name = var.project_name
  environment  = var.environment
  cluster_name = var.cluster_name
  vpc_cidr     = "10.0.0.0/16"
}

# ─── EKS Cluster ─────────────────────────────────────────────────────────────

module "eks" {
  source = "../../modules/eks-cluster"

  cluster_name        = var.cluster_name
  kubernetes_version  = var.kubernetes_version
  subnet_ids          = module.vpc.private_subnet_ids
  vpc_id              = module.vpc.vpc_id
  node_instance_types = ["t3.medium", "t3a.medium"]
  node_desired_size   = 1
  node_min_size       = 0
  node_max_size       = 2
  environment         = var.environment
}

# ─── RDS PostgreSQL ──────────────────────────────────────────────────────────

module "rds" {
  source = "../../modules/rds-postgres"

  identifier     = local.db_identifier
  db_name        = "northcare"
  username       = "northcare_admin"
  subnet_ids     = module.vpc.private_subnet_ids
  vpc_id         = module.vpc.vpc_id
  eks_node_sg_id = module.eks.cluster_security_group_id
  environment    = var.environment
  instance_class = "db.t3.micro"
}

# ─── Weekday Scheduler (10am–4pm EST) ────────────────────────────────────────

module "scheduler" {
  source = "../../modules/scheduler-lambda"

  cluster_name   = module.eks.cluster_name
  nodegroup_name = module.eks.node_group_name
  db_instance_id = local.db_identifier
  desired_count  = 1
  environment    = var.environment
}
