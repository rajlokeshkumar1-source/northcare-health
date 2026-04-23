locals {
  common_tags = {
    Project     = "northcare"
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = "northcare-health"
  }

  function_name = "${var.environment}-northcare-scheduler"
}

# ─── Lambda source bundle (inline Python) ────────────────────────────────────

data "archive_file" "lambda_zip" {
  type        = "zip"
  output_path = "${path.module}/scheduler_payload.zip"

  source {
    filename = "lambda_function.py"
    content  = <<-PYTHON
      import boto3
      import os
      import logging

      logger = logging.getLogger()
      logger.setLevel(logging.INFO)


      def handler(event, context):
          """
          Entry point invoked by EventBridge.
          Expects event payload: {"action": "scale_up"} or {"action": "scale_down"}
          """
          action         = event.get("action", "")
          cluster_name   = os.environ["CLUSTER_NAME"]
          nodegroup_name = os.environ["NODEGROUP_NAME"]
          db_instance_id = os.environ["DB_INSTANCE_ID"]
          desired_count  = int(os.environ.get("DESIRED_COUNT", "1"))

          eks_client = boto3.client("eks")
          rds_client = boto3.client("rds")

          logger.info("NorthCare scheduler triggered — action: %s", action)

          if action == "scale_up":
              scale_up_eks(eks_client, cluster_name, nodegroup_name, desired_count)
              start_rds(rds_client, db_instance_id)
          elif action == "scale_down":
              scale_down_eks(eks_client, cluster_name, nodegroup_name)
              stop_rds(rds_client, db_instance_id)
          else:
              logger.warning("Unknown action '%s' — expected scale_up or scale_down", action)

          return {"statusCode": 200, "body": "action={} completed".format(action)}


      def scale_up_eks(eks_client, cluster_name, nodegroup_name, desired_count):
          try:
              eks_client.update_nodegroup_config(
                  clusterName=cluster_name,
                  nodegroupName=nodegroup_name,
                  scalingConfig={
                      "desiredSize": desired_count,
                      "minSize": 1,
                  },
              )
              logger.info(
                  "EKS node group %s scaled up to %d nodes", nodegroup_name, desired_count
              )
          except Exception as exc:
              logger.error("Failed to scale up EKS node group %s: %s", nodegroup_name, exc)
              raise


      def scale_down_eks(eks_client, cluster_name, nodegroup_name):
          try:
              eks_client.update_nodegroup_config(
                  clusterName=cluster_name,
                  nodegroupName=nodegroup_name,
                  scalingConfig={
                      "desiredSize": 0,
                      "minSize": 0,
                  },
              )
              logger.info("EKS node group %s scaled down to 0 nodes", nodegroup_name)
          except Exception as exc:
              logger.error("Failed to scale down EKS node group %s: %s", nodegroup_name, exc)
              raise


      def start_rds(rds_client, db_instance_id):
          try:
              rds_client.start_db_instance(DBInstanceIdentifier=db_instance_id)
              logger.info("RDS start initiated for %s", db_instance_id)
          except rds_client.exceptions.InvalidDBInstanceStateFault:
              logger.warning("RDS %s already running — skipping start", db_instance_id)
          except Exception as exc:
              logger.error("Failed to start RDS %s: %s", db_instance_id, exc)
              raise


      def stop_rds(rds_client, db_instance_id):
          try:
              rds_client.stop_db_instance(DBInstanceIdentifier=db_instance_id)
              logger.info("RDS stop initiated for %s", db_instance_id)
          except rds_client.exceptions.InvalidDBInstanceStateFault:
              logger.warning("RDS %s already stopped — skipping stop", db_instance_id)
          except Exception as exc:
              logger.error("Failed to stop RDS %s: %s", db_instance_id, exc)
              raise
    PYTHON
  }
}

# ─── Lambda IAM Role ─────────────────────────────────────────────────────────

resource "aws_iam_role" "lambda_scheduler" {
  name = "${local.function_name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })

  tags = local.common_tags
}

resource "aws_iam_role_policy" "lambda_scheduler" {
  name = "${local.function_name}-policy"
  role = aws_iam_role.lambda_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "EKSNodeGroupScaling"
        Effect = "Allow"
        Action = [
          "eks:UpdateNodegroupConfig",
          "eks:DescribeNodegroup",
        ]
        # Wildcard on the UUID suffix (AWS-generated, unknown at plan time)
        Resource = "arn:aws:eks:*:*:nodegroup/${var.cluster_name}/${var.nodegroup_name}/*"
      },
      {
        Sid    = "RDSStartStop"
        Effect = "Allow"
        Action = [
          "rds:StartDBInstance",
          "rds:StopDBInstance",
          "rds:DescribeDBInstances",
        ]
        Resource = "arn:aws:rds:*:*:db:${var.db_instance_id}"
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
    ]
  })
}

# ─── Lambda Function ─────────────────────────────────────────────────────────

resource "aws_lambda_function" "scheduler" {
  filename         = data.archive_file.lambda_zip.output_path
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  function_name    = local.function_name
  role             = aws_iam_role.lambda_scheduler.arn
  handler          = "lambda_function.handler"
  runtime          = "python3.11"
  timeout          = 60

  environment {
    variables = {
      CLUSTER_NAME    = var.cluster_name
      NODEGROUP_NAME  = var.nodegroup_name
      DB_INSTANCE_ID  = var.db_instance_id
      DESIRED_COUNT   = tostring(var.desired_count)
    }
  }

  tags = local.common_tags
}

# ─── EventBridge Rules ───────────────────────────────────────────────────────

# 10:00 EST = 14:00 UTC
resource "aws_cloudwatch_event_rule" "scale_up" {
  name                = "${local.function_name}-scale-up"
  description         = "Start NorthCare: scale EKS to ${var.desired_count} node(s) and start RDS at 10am EST weekdays"
  schedule_expression = "cron(0 14 ? * MON-FRI *)"
  state               = "ENABLED"
  tags                = local.common_tags
}

# 16:00 EST = 20:00 UTC
resource "aws_cloudwatch_event_rule" "scale_down" {
  name                = "${local.function_name}-scale-down"
  description         = "Stop NorthCare: scale EKS to 0 nodes and stop RDS at 4pm EST weekdays"
  schedule_expression = "cron(0 20 ? * MON-FRI *)"
  state               = "ENABLED"
  tags                = local.common_tags
}

# ─── EventBridge Targets ─────────────────────────────────────────────────────

resource "aws_cloudwatch_event_target" "scale_up" {
  rule = aws_cloudwatch_event_rule.scale_up.name
  arn  = aws_lambda_function.scheduler.arn
  input = jsonencode({ action = "scale_up" })
}

resource "aws_cloudwatch_event_target" "scale_down" {
  rule = aws_cloudwatch_event_rule.scale_down.name
  arn  = aws_lambda_function.scheduler.arn
  input = jsonencode({ action = "scale_down" })
}

# ─── Lambda Resource Policies ────────────────────────────────────────────────

resource "aws_lambda_permission" "allow_scale_up" {
  statement_id  = "AllowEventBridgeScaleUp"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.scheduler.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.scale_up.arn
}

resource "aws_lambda_permission" "allow_scale_down" {
  statement_id  = "AllowEventBridgeScaleDown"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.scheduler.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.scale_down.arn
}
