.PHONY: setup up up-observability down reset test test-backend test-frontend lint format load-test

setup:
	cd frontend && npm ci

up:
	docker compose up --build

up-observability:
	docker compose --profile observability up --build

down:
	docker compose down

reset:
	docker compose down -v

test: test-backend test-frontend

test-backend:
	cd backend && ./mvnw verify

test-frontend:
	cd frontend && npm test -- --run && npm run typecheck && npm run build

lint:
	cd frontend && npm run lint

format:
	cd frontend && npm run format

load-test:
	docker run --rm --network taskforge_default -e TASKFORGE_BASE_URL=http://control-plane:8080 -e TASKFORGE_K6_VUS=$${TASKFORGE_K6_VUS:-3} -e TASKFORGE_K6_DURATION=$${TASKFORGE_K6_DURATION:-20s} -v "$(CURDIR)/load-tests/k6:/scripts" grafana/k6:2.2.0 run /scripts/workflow-smoke.js
