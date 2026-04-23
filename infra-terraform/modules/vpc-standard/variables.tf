variable "project_name" {
  description = "Name of the project; used for resource naming and tagging."
  type        = string
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod)."
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster name used to set the kubernetes.io/cluster/* subnet tags required by the AWS cloud controller."
  type        = string
}

variable "vpc_cidr" {
  description = "Primary IPv4 CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}
