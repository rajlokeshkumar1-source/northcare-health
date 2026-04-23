variable "cluster_name" {
  description = "EKS cluster name — used in IAM policy ARNs and Lambda env vars."
  type        = string
}

variable "nodegroup_name" {
  description = "EKS managed node group name — used in IAM policy ARNs and Lambda env vars."
  type        = string
}

variable "db_instance_id" {
  description = "RDS DB instance identifier — used in IAM policy ARNs and Lambda env vars."
  type        = string
}

variable "desired_count" {
  description = "Number of EKS nodes to launch when scaling up (scale-down always targets 0)."
  type        = number
  default     = 1
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod)."
  type        = string
}
