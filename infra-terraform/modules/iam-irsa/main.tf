locals {
  # Strip the https:// prefix that IAM condition keys require
  oidc_host = replace(var.oidc_provider_url, "https://", "")
}

# ─── Trust Policy ────────────────────────────────────────────────────────────

data "aws_iam_policy_document" "irsa_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    # Restrict to a specific Kubernetes service account
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_host}:sub"
      values   = ["system:serviceaccount:${var.namespace}:${var.service_account_name}"]
    }

    # Ensure the token audience is STS — prevents confused-deputy attacks
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

# ─── IAM Role ────────────────────────────────────────────────────────────────

resource "aws_iam_role" "irsa" {
  name               = var.role_name
  assume_role_policy = data.aws_iam_policy_document.irsa_assume_role.json

  tags = {
    ManagedBy = "terraform"
    Owner     = "northcare-health"
  }
}

# ─── Policy Attachments ──────────────────────────────────────────────────────

resource "aws_iam_role_policy_attachment" "irsa" {
  count      = length(var.policy_arns)
  role       = aws_iam_role.irsa.name
  policy_arn = var.policy_arns[count.index]
}
