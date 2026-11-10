# Configuration des fournisseurs (Providers) Terraform
terraform {
  required_version = ">= 1.0.0"

  required_providers {
    openstack = {
      source  = "terraform-provider-openstack/openstack"
      version = "~> 1.54.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.4.0"
    }
  }
}

# Authentification sécurisée via clouds.yaml (Zéro secret en dur)
provider "openstack" {
  cloud = var.cloud_name
}
