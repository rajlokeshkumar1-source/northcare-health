output "cluster_name" {
  description = "Name of the EKS cluster."
  value       = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  description = "API server endpoint URL of the EKS cluster."
  value       = aws_eks_cluster.main.endpoint
}

output "cluster_ca_certificate" {
  description = "Base64-encoded certificate authority data for the EKS cluster."
  value       = aws_eks_cluster.main.certificate_authority[0].data
  sensitive   = true
}

output "cluster_security_group_id" {
  description = "ID of the cluster security group created by EKS (attached to managed nodes)."
  value       = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

output "oidc_provider_arn" {
  description = "ARN of the IAM OIDC provider — used by IRSA module."
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_provider_url" {
  description = "HTTPS URL of the IAM OIDC provider — used by IRSA module."
  value       = aws_iam_openid_connect_provider.eks.url
}

output "node_group_name" {
  description = "Name of the managed node group — used by the scheduler Lambda."
  value       = aws_eks_node_group.main.node_group_name
}

output "node_group_role_arn" {
  description = "ARN of the IAM role assumed by worker nodes."
  value       = aws_iam_role.eks_node_group.arn
}
