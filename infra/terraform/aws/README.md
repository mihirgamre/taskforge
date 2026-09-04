# TaskForge AWS Terraform

This directory is an AWS deployment blueprint for M7. It intentionally contains no credentials or secret values.

## Architecture

- CloudFront + private S3 bucket for the built React frontend.
- Public ALB exposing the control-plane API.
- ECS Fargate services for control-plane, scheduler, and worker in private subnets.
- RDS PostgreSQL with AWS-managed master password in private subnets.
- ElastiCache Redis for non-authoritative cache/rate-limit infrastructure in private subnets.
- NAT egress for private backend services.
- Managed Kafka/MSK supplied through `kafka_bootstrap_servers`.
- ECR repositories with scan-on-push enabled.
- CloudWatch log groups for backend services.

## First Deployment

1. Create a Secrets Manager secret for `TASKFORGE_AUTH_JWT_SECRET`.
2. Provision or select MSK/MSK Serverless and capture bootstrap brokers.
3. Copy `terraform.tfvars.example` to an ignored local `terraform.tfvars`.
4. Set real values for `frontend_bucket_name`, `image_tag`, `jwt_secret_arn`, and `kafka_bootstrap_servers`.
5. Run:

```bash
terraform init
terraform plan
terraform apply
```

`terraform.tfvars` must never be committed.
