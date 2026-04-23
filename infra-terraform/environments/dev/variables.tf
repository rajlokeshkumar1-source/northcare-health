variable "project_name" {
  description = "Project name used for resource naming and tagging."
  type        = string
  default     = "northcare"
}

variable "environment" {
  description = "Deployment environment."
  type        = string
  default     = "dev"
}

variable "cluster_name" {
  description = "EKS cluster name."
  type        = string
  default     = "northcare-dev"
}

variable "region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS cluster."
  type        = string
  default     = "1.29"
}
