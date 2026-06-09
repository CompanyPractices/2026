# Gateway Service

Gateway Service - точка входа во внешнее REST API. Принимает запросы от симуляторов 
и dashboard, валидирует входящие транзакции, применяет rate limit и проксирует запросы в нужные downstream-сервисы.

## Что делает сервис

- Проверяет состояние gateway и downstream-сервисов через `GET /health`.
- Принимает авторизационные транзакции на `POST /api/transactions`.
- Валидирует `AuthorizationRequest` перед отправкой в свич.
- Ограничивает нагрузку на `POST /api/transactions` лимитом.
- Проксирует REST-запросы в Switch, Card Management, Transaction Logger, Terminal Simulator и Merchant Simulator.
- Возвращает JSON-ошибки для validation/rate-limit/downstream-failure сценариев.
- Добавляет `X-Request-Id` в запрос и ответ, а также пишет request log.

## Технологии

- Java 21
- Spring Boot 3.4
- Spring Cloud Gateway MVC
- Spring Boot Actuator
- springdoc-openapi
- Maven
- Docker

## Основные endpoint-ы

| Метод | Путь | Назначение | Downstream |
|---|---|---|---|
| `GET` | `/health` | Health-check Gateway и зависимых сервисов | Gateway |
| `POST` | `/api/transactions` | Прием и валидация авторизационной транзакции | Switch: `/api/internal/route` |
| `GET/POST/...` | `/api/cards/**` | Операции с картами | Card Management |
| `GET` | `/api/transactions/search` | Поиск транзакций | Transaction Logger |
| `GET/POST/...` | `/api/dashboard/**` | Данные для dashboard | Transaction Logger |
| `GET/POST/...` | `/api/simulator/terminal/**` | Terminal Simulator API | Terminal Simulator |
| `GET/POST/...` | `/api/simulator/merchant/**` | Merchant Simulator API | Merchant Simulator |

Swagger UI доступен по стандартному springdoc-пути:

```text
http://localhost:8080/docs
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Валидация транзакций

`POST /api/transactions` принимает `AuthorizationRequest` из общего модуля `common`.

Пример запроса:

```json
{
  "mti": "0100",
  "stan": "000001",
  "pan": "4000003458730237",
  "processingCode": "000000",
  "amount": 72472,
  "currencyCode": "643",
  "transmissionDateTime": "2026-06-05T18:12:49.07",
  "terminalId": "TERM001",
  "terminalType": "POS",
  "merchantId": "MERCH00000000029",
  "mcc": "5045",
  "acquirerId": "ACQ002"
}
```

Gateway проверяет:

- обязательные строковые поля не пустые;
- `mti` равен `0100`;
- `pan` состоит ровно из 16 цифр;
- `amount` передан и больше `0`;
- `currencyCode` содержит ровно 3 символа;
- `mcc` состоит ровно из 4 цифр.

Если валидация не проходит, Gateway возвращает `400 Bad Request`:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Field 'pan' must be exactly 16 digits",
  "timestamp": "2026-06-01T10:30:00Z",
  "details": null,
  "path": null
}
```

## Rate limiting

Для `POST /api/transactions` включен in-memory rate limit. По умолчанию разрешено 100 запросов в секунду.

При превышении лимита Gateway возвращает `429 Too Many Requests`:

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Try again later.",
  "retryAfterMs": 1000
}
```

Также выставляется HTTP-заголовок:

```text
Retry-After: 1
```

## Downstream errors

Если downstream-сервис недоступен или не отвечает по сети, Gateway возвращает `503 Service Unavailable`.

Пример ответа:

```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "switch service is temporarily unavailable",
  "serviceName": "switch"
}
```

Имя сервиса определяется по gateway route metadata `serviceName` из `application.yml`.

## Логирование и трассировка

На каждый запрос Gateway добавляет или переиспользует заголовок:

```text
X-Request-Id: <uuid или входящее значение>
```

Этот же идентификатор возвращается в ответе и кладется в MDC. В лог пишется JSON с базовой информацией о запросе:

```json
{
  "requestId": "0f7d08ec-17b4-4d56-85e3-3139e40001ab",
  "method": "POST",
  "path": "/api/transactions",
  "responseCode": 200,
  "responseTime": 37
}
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
| `TRANSACTIONS_RATE_LIMIT` | `100` | Максимум `POST /api/transactions` запросов в секунду. |

## Локальный запуск

Из папки `services`:

```bash
mvn -P gateway -pl gateway -am spring-boot:run
```

Или собрать jar:

```bash
mvn -P gateway -pl gateway -am package
java -jar gateway/target/gateway-*.jar
```

Пример запуска с адресами downstream-сервисов:

```bash
SWITCH_URL=http://localhost:8082 \
LOGGER_URL=http://localhost:8088 \
AUTH_URL=http://localhost:8083 \
CARD_MGMT_URL=http://localhost:8081 \
mvn -P gateway -pl gateway -am spring-boot:run
```

## Docker

Сборка образа из корня репозитория:

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

## Проверка

Health-check:

```bash
curl http://localhost:8080/health
```

Тестовая транзакция:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "mti": "0100",
    "stan": "000001",
    "pan": "4000003458730237",
    "processingCode": "000000",
    "amount": 72472,
    "currencyCode": "643",
    "transmissionDateTime": "2026-06-05T18:12:49.07",
    "terminalId": "TERM001",
    "terminalType": "POS",
    "merchantId": "MERCH00000000029",
    "mcc": "5045",
    "acquirerId": "ACQ002"
  }'
```

Запуск тестов:

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
