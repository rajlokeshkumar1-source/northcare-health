output "vpc_id" {
  description = "ID of the VPC."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Ordered list of public subnet IDs (us-east-1a, us-east-1b)."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Ordered list of private subnet IDs for EKS nodes and RDS (us-east-1a, us-east-1b)."
  value       = aws_subnet.private[*].id
}

output "vpc_cidr_block" {
  description = "Primary CIDR block of the VPC."
  value       = aws_vpc.main.cidr_block
}
