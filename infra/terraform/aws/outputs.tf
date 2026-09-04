output "control_plane_url" {
  description = "Public control-plane URL."
  value       = "http://${aws_lb.main.dns_name}"
}

output "frontend_distribution_domain" {
  description = "CloudFront domain for the frontend."
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "ecr_repositories" {
  description = "Backend service ECR repository URLs."
  value       = { for name, repo in aws_ecr_repository.service : name => repo.repository_url }
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.main.name
}
