# Gateway Service

Gateway Service - точка входа во внешнее REST API процессинговой системы. Сервис
принимает запросы от симуляторов и dashboard-а, валидирует входящие запросы, ограничивает количество запросов, добавляет трассировку, защищает downstream-сервисы circuit breaker'ом
и проксирует запросы в нужные сервисы.

Подробные контракты API, модели запросов/ответов, коды ошибок и правила валидации
описаны в OpenAPI-спецификации.

## Возможности

- Health-check самого gateway и зависимых сервисов.
- Reverse proxy для основных сервисов процессинга.
- Валидация авторизационных транзакций перед отправкой в Switch.
- In-memory rate limiting для транзакционного endpoint'а.
- Circuit breaker для downstream-сервисов.
- Кэширование успешных GET-ответов Card Management.
- Единая обработка недоступности downstream-сервисов.
- Request logging с `X-Request-Id` и MDC.
- Метрики Actuator и экспорт в Prometheus.

## API-документация

После запуска сервиса документация доступна в Swagger UI:

```text
http://localhost:8080/docs
```

Swagger UI показывает спецификации Gateway, Switch, Transaction Logger,
Terminal Simulator, Merchant Acquirer и Card Management.

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
| `CIRCUIT_BREAKER_FAILURE_THRESHOLD` | `3` | Количество ошибок до открытия circuit breaker. |
| `CIRCUIT_BREAKER_OPEN_DURATION` | `10s` | Время, на которое circuit breaker остается открытым. |
| `GATEWAY_PUBLIC_URL` | `http://localhost:${GATEWAY_PORT:8080}` | Публичный URL, который подставляется в OpenAPI-документы. |

Маршруты, metadata downstream-сервисов и rewrite-правила находятся в
`src/main/resources/application.yml`.

Дополнительные настройки health-check:

| Свойство | Значение по умолчанию | Описание |
|---|---:|---|
| `gateway.health.connection-timeout` | `5` | Timeout подключения health HTTP-клиента, секунды. |
| `gateway.health.request-timeout` | `3` | Timeout health-запроса, секунды. |
| `gateway.health.url` | `/health` | Путь health-check на downstream-сервисах. |

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

Prometheus metrics:

```text
http://localhost:8080/actuator/prometheus
```

## Структура модуля

```text
src/main/java/com/processing/gateway
├── config/        # общие Spring beans
├── circuitbreaker/ # in-memory circuit breaker
├── client/        # HTTP clients for downstream checks
├── controller/    # REST controllers
├── enums/         # gateway enums
├── filter/        # logging, rate limit, validation, cache, circuit breaker
├── logging/       # модель структурированного request log
├── properties/    # binding application.yml properties
├── ratelimit/     # in-memory limiter
├── service/       # health-check и route/service resolution
├── validation/    # validation rules for AuthorizationRequest
└── wrapper/       # request wrapper for mutable headers
```
