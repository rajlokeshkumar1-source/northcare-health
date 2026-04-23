output "db_endpoint" {
  description = "Hostname of the RDS instance (without port)."
  value       = aws_db_instance.main.address
}

output "db_port" {
  description = "TCP port the RDS instance listens on."
  value       = aws_db_instance.main.port
}

output "db_name" {
  description = "Name of the database created on the RDS instance."
  value       = aws_db_instance.main.db_name
}

output "secret_arn" {
  description = "ARN of the Secrets Manager secret that holds the full DB connection bundle (host, port, user, password, dbname)."
  value       = aws_secretsmanager_secret.db_credentials.arn
}
