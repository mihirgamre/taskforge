variable "aws_region" {
  description = "AWS region for the TaskForge environment."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "prod"
}

variable "image_tag" {
  description = "Container image tag deployed to all TaskForge backend services."
  type        = string
}

variable "frontend_bucket_name" {
  description = "Globally unique S3 bucket name for built frontend assets."
  type        = string
}

variable "jwt_secret_arn" {
  description = "Secrets Manager ARN containing TASKFORGE_AUTH_JWT_SECRET."
  type        = string
}

variable "kafka_bootstrap_servers" {
  description = "Managed Kafka/MSK bootstrap servers."
  type        = string
}

variable "allowed_http_cidr_blocks" {
  description = "CIDR blocks allowed to reach the public load balancer."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "database_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "database_allocated_storage_gb" {
  description = "Allocated RDS PostgreSQL storage in GiB."
  type        = number
  default     = 20
}

variable "service_cpu" {
  description = "Fargate task CPU units per backend service."
  type        = number
  default     = 512
}

variable "service_memory" {
  description = "Fargate task memory MiB per backend service."
  type        = number
  default     = 1024
}
