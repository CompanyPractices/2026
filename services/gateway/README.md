# Gateway Service

Gateway Service - точка входа во внешнее REST API процессинговой системы. Сервис
принимает запросы от симуляторов и dashboard, валидирует входящий поток, применяет
rate limit, добавляет трассировку и проксирует запросы в downstream-сервисы.

Подробные контракты API, модели запросов/ответов, коды ошибок и правила валидации
описаны в OpenAPI-спецификации.

## Возможности

- Health-check самого gateway и зависимых сервисов.
- Reverse proxy для основных сервисов процессинга.
- Валидация авторизационных транзакций перед отправкой в Switch.
- In-memory rate limiting для транзакционного endpoint'а.
- Единая обработка недоступности downstream-сервисов.
- Request logging с `X-Request-Id` и MDC.
- OpenAPI/Swagger UI через springdoc.

## Технологии

- Java 21
- Spring Boot 3.4
- Spring Cloud Gateway MVC
- Spring Boot Actuator
- springdoc-openapi
- Maven
- Docker

## API-документация

После запуска сервиса документация доступна в Swagger UI:

```text
http://localhost:8080/docs
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Конфигурация

Основные переменные окружения:

| Переменная | Значение по умолчанию | Описание |
|---|---:|---|
| `SERVER_PORT` | `8080` | Порт Gateway. Имеет приоритет над `GATEWAY_PORT`. |
| `GATEWAY_PORT` | `8080` | Альтернативная настройка порта Gateway. |
| `SWITCH_URL` | `http://localhost:8082` | URL Switch Service. |
| `LOGGER_URL` | `http://localhost:8088` | URL Transaction Logger. |
| `AUTH_URL` | `http://localhost:8083` | URL Authorization Service для health-check. |
| `CARD_MGMT_URL` | `http://localhost:8081` | URL Card Management Service. |
| `TERMINAL_SIM_URL` | `http://localhost:8085` | URL Terminal Simulator. |
| `MERCHANT_SIM_URL` | `http://localhost:8086` | URL Merchant Simulator. |
| `TRANSACTIONS_RATE_LIMIT` | `100` | Лимит транзакционных запросов в секунду. |

Маршруты, metadata downstream-сервисов и rewrite-правила находятся в
`src/main/resources/application.yml`.

## Локальный запуск

Из папки `services`:

```bash
mvn -P gateway -pl gateway -am spring-boot:run
```

Сборка jar:

```bash
mvn -P gateway -pl gateway -am package
```

Запуск собранного jar:

```bash
java -jar gateway/target/gateway-*.jar
```

Пример запуска с явными адресами downstream-сервисов:

```bash
SWITCH_URL=http://localhost:8082 \
LOGGER_URL=http://localhost:8088 \
AUTH_URL=http://localhost:8083 \
CARD_MGMT_URL=http://localhost:8081 \
mvn -P gateway -pl gateway -am spring-boot:run
```

## Docker

Сборка образа выполняется из корня репозитория:

```bash
docker build -f services/gateway/Dockerfile -t processing-gateway .
```

Запуск контейнера:

```bash
docker run --rm -p 8080:8080 \
  -e SWITCH_URL=http://host.docker.internal:8082 \
  -e LOGGER_URL=http://host.docker.internal:8088 \
  -e AUTH_URL=http://host.docker.internal:8083 \
  -e CARD_MGMT_URL=http://host.docker.internal:8081 \
  -e TERMINAL_SIM_URL=http://host.docker.internal:8085 \
  -e MERCHANT_SIM_URL=http://host.docker.internal:8086 \
  processing-gateway
```

## Проверка и тесты

Быстрая проверка запущенного сервиса:

```bash
curl http://localhost:8080/health
```

Unit-тесты:

```bash
mvn -P gateway -pl gateway -am test
```

## Структура модуля

```text
src/main/java/com/processing/gateway
├── config/        # общие Spring beans
├── controller/    # REST controllers
├── filter/        # request logging, rate limit, validation, downstream errors
├── logging/       # модель структурированного request log
├── properties/    # binding application.yml properties
├── ratelimit/     # in-memory limiter
├── service/       # health-check и route/service resolution
├── validation/    # validation rules for AuthorizationRequest
└── wrapper/       # request wrapper for mutable headers
```
