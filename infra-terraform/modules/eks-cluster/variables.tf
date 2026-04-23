variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS cluster (e.g. '1.29')."
  type        = string
  default     = "1.29"
}

variable "subnet_ids" {
  description = "List of subnet IDs (private recommended) for the EKS control plane and managed node group."
  type        = list(string)
}

variable "vpc_id" {
  description = "VPC ID in which the EKS cluster lives."
  type        = string
}

variable "node_instance_types" {
  description = "EC2 instance types for SPOT managed node group. Multiple types increase SPOT availability."
  type        = list(string)
  default     = ["t3.medium", "t3a.medium"]
}

variable "node_desired_size" {
  description = "Initial desired number of worker nodes. The Lambda scheduler will override this at runtime."
  type        = number
  default     = 1
}

variable "node_min_size" {
  description = "Minimum number of worker nodes (0 allows full scale-down by the scheduler)."
  type        = number
  default     = 0
}

variable "node_max_size" {
  description = "Maximum number of worker nodes for scale-out headroom."
  type        = number
  default     = 3
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod)."
  type        = string
}
