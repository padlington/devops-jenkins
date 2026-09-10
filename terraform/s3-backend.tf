# backend.tf

terraform {
  backend "s3" {
    bucket         = "my-company-terraform-state" # The exact name of your S3 bucket
    key            = "production/network.tfstate" # The file path within the bucket where state is saved
    region         = "us-east-1"                  # AWS region where the bucket resides
    encrypt        = true                         # Ensures state is encrypted at rest
    use_lockfile   = true                         # Enables S3 Native State Locking (Terraform 1.9+)
  }
}
