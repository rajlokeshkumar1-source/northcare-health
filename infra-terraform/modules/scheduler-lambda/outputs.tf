output "lambda_function_name" {
  description = "Name of the EventBridge-triggered Lambda scheduler."
  value       = aws_lambda_function.scheduler.function_name
}

output "scale_up_rule_arn" {
  description = "ARN of the EventBridge rule that fires the scale-up action (10am EST weekdays)."
  value       = aws_cloudwatch_event_rule.scale_up.arn
}

output "scale_down_rule_arn" {
  description = "ARN of the EventBridge rule that fires the scale-down action (4pm EST weekdays)."
  value       = aws_cloudwatch_event_rule.scale_down.arn
}
