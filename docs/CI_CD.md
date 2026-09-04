# CI/CD

## Current Validation

- Backend Maven verification.
- Frontend install, lint, typecheck, tests, and build.
- Docker image builds for frontend, control-plane, scheduler, and worker.
- `npm audit --audit-level=high`.
- GitHub dependency review on pull requests.
- CodeQL analysis for Java and TypeScript/JavaScript.

## Deployment

`.github/workflows/deploy.yml` is manual. It expects:

- `AWS_ROLE_TO_ASSUME`
- `AWS_REGION`
- `ECS_CLUSTER_NAME`
- `FRONTEND_BUCKET_NAME`
- optional `CLOUDFRONT_DISTRIBUTION_ID`
- ECR repositories created by Terraform
- a Git SHA image tag

The workflow does not store AWS credentials in the repository.
