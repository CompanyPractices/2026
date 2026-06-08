# переменные
DOCKER_COMPOSE = docker compose
MVN = mvn -f services/pom.xml
NPM_DIR = services/dashboard
SERVICE ?= gateway

# специальные цели
.PHONY: run stop clean build test lint smoke

help:
	@echo Available commands:
	@echo.
	@echo Docker:
	@echo   make run           - Up all containers
	@echo   make stop          - Stop all containers
	@echo   make clean         - Remove containers, images and volumes
	@echo   make build         - Rebuild all images
	@echo   make status        - Containers status
	@echo   make logs          - Show logs (SERVICE=gateway)
	@echo   make smoke         - Run smoke tests
	@echo.
	@echo Maven (all modules):
	@echo   make mvn-all       - Build all modules
	@echo   make mvn-clean     - Clean all modules
	@echo   make mvn-test      - Run all tests
	@echo   make mvn-lint      - Run checkstyle
	@echo.
	@echo Maven (single service, SERVICE=gateway):
	@echo   make mvn-service   - Build one service + deps
	@echo.
	@echo Frontend (dashboard):
	@echo   make npm-install   - Install npm dependencies
	@echo   make npm-lint      - Run ESLint
	@echo   make npm-test      - Run tests
	@echo   make npm-build     - Build production bundle

# Docker
run:
	$(DOCKER_COMPOSE) up -d

stop:
	$(DOCKER_COMPOSE) down

clean:
	$(DOCKER_COMPOSE) down -v --remove-orphans

build:
	$(DOCKER_COMPOSE) build

status:
	$(DOCKER_COMPOSE) ps

logs:
	$(DOCKER_COMPOSE) logs -f $(SERVICE)

smoke:
	pwsh -ExecutionPolicy Bypass -File scripts/smoke-test.ps1

# Maven
mvn-all:
	$(MVN) package

mvn-clean:
	$(MVN) clean

mvn-test:
	$(MVN) test

mvn-lint:
	$(MVN) checkstyle:check

mvn-service:
	$(MVN) -P$(SERVICE) package

# Frontend
npm-install:
	cd $(NPM_DIR) && npm install

npm-lint:
	cd $(NPM_DIR) && npm run lint

npm-test:
	cd $(NPM_DIR) && npm test -- --run

npm-build:
	cd $(NPM_DIR) && npm run build
