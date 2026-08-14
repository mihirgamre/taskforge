.PHONY: setup up down reset test test-backend test-frontend lint format

setup:
	cd frontend && npm ci

up:
	docker compose up --build

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

