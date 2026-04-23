terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.27"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }

  # ── Local backend (default for dev) ─────────────────────────────────────────
  # State is stored in terraform.tfstate in this directory.
  # To migrate to Terraform Cloud / HCP Terraform, replace this block with:
  #
  # cloud {
  #   organization = "northcare"
  #   workspaces {
  #     name = "northcare-dev"
  #   }
  # }
  #
  # Then run: terraform login && terraform init
  backend "local" {}
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Owner       = "northcare-health"
    }
  }
}
