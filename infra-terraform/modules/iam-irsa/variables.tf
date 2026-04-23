variable "role_name" {
  description = "Name of the IAM role to create."
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider (output of the eks-cluster module)."
  type        = string
}

variable "oidc_provider_url" {
  description = "HTTPS URL of the EKS OIDC provider (output of the eks-cluster module)."
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace where the service account lives."
  type        = string
}

variable "service_account_name" {
  description = "Kubernetes service account name that will assume this IAM role."
  type        = string
}

variable "policy_arns" {
  description = "List of IAM policy ARNs to attach to the role. Can be AWS-managed or customer-managed."
  type        = list(string)
  default     = []
}
