# ADR 0012 - Local Observability With Prometheus And Grafana

## Status

Accepted.

## Context

TaskForge needs production-reliability foundations before cloud deployment. The current backend services already use Spring Boot Actuator and Micrometer, so Prometheus-compatible metrics are the simplest useful next step.

## Decision

Use Spring Actuator Prometheus endpoints in every backend service and add optional Docker Compose services for Prometheus and Grafana under the `observability` profile.

Request logs include request method, path, status, duration, and request id. They do not include request bodies, query strings, authorization headers, API keys, or task payloads.

## Consequences

Local developers can start the application stack with observability using:

```bash
docker compose --profile observability up --build
```

This does not replace production tracing, alerting, dashboard design, or cloud logging. Those remain deployment-hardening work.
