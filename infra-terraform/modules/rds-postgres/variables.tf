variable "identifier" {
  description = "Unique RDS instance identifier; also used for the subnet group and security group names."
  type        = string
}

variable "db_name" {
  description = "Name of the initial database to create inside the instance."
  type        = string
}

variable "username" {
  description = "Master username for the PostgreSQL instance."
  type        = string
}

variable "subnet_ids" {
  description = "Private subnet IDs for the RDS DB subnet group."
  type        = list(string)
}

variable "vpc_id" {
  description = "VPC ID in which the RDS instance will be deployed."
  type        = string
}

variable "eks_node_sg_id" {
  description = "Security group ID of the EKS managed node group; granted inbound access on port 5432."
  type        = string
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod)."
  type        = string
}

variable "instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t3.micro"
}
