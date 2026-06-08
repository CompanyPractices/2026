# Transaction Logger

## Назначение

Transaction Logger сохраняет все транзакции процессинга, предоставляет API для поиска и фильтрации, агрегированную статистику для Dashboard и real-time поток через WebSocket.

---

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** PostgreSQL
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check сервиса |
| POST | `/api/internal/log` | Приём транзакции от Switch и сохранение в PostgreSQL |
| GET | `/api/transactions/search` | Поиск транзакций с фильтрацией и пагинацией |
| GET | `/api/dashboard/stats` | Агрегированная статистика |
| GET | `/api/dashboard/recent` | Последние N транзакций |
| WS | `/ws/transactions` | Real-time поток транзакций |

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "ok",
  "service": "transaction-logger",
  "dependencies": {}
}
```

---

#### `POST /api/internal/log`

Endpoint принимает транзакцию от Switch и сохраняет её в PostgreSQL.

Если запрос повторяется с уже существующим `id` и все поля совпадают с сохранённой транзакцией, новая запись не создаётся, а сервис возвращает существующую транзакцию.

**Тело запроса:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mti": "0100",
  "stan": "000001",
  "rrn": "012345678901",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 150000,
  "currencyCode": "643",
  "terminalId": "TERM001",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001",
  "issuerId": "ISS001",
  "acquiringFee": 2250,
  "status": "APPROVED",
  "declineReason": null,
  "authCode": "ABC123",
  "processingTimeMs": 42,
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "createdAt": "2026-06-01T10:30:01Z"
}
```

**Ответ 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "stored"
}
```

**Ответ 200:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mti": "0100",
  "stan": "000001",
  "rrn": "012345678901",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 150000,
  "currencyCode": "643",
  "terminalId": "TERM001",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001",
  "issuerId": "ISS001",
  "acquiringFee": 2250,
  "status": "APPROVED",
  "declineReason": null,
  "authCode": "ABC123",
  "processingTimeMs": 42,
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "createdAt": "2026-06-01T10:30:01Z"
}
```

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | Невалидные поля запроса или некорректное JSON-тело |
| 409 | Транзакция с таким `id` уже существует, но поля отличаются |
| 503 | Ошибка доступа к PostgreSQL |
| 500 | Внутренняя ошибка сервиса |

**Пример ошибки:**
```json
{
  "error": "Transaction conflict",
  "message": "Transaction with id 550e8400-e29b-41d4-a716-446655440000 already exists with different data",
  "timestamp": "2026-06-05T08:30:00Z",
  "serviceName": "transaction-logger",
  "retryAfterMs": "0"
}
```

---

#### `GET /api/transactions/search`

**Query-параметры:**

| Параметр | Тип | Описание                        | По умолчанию |
|----------|-----|---------------------------------|--------------|
| `pan` | string | Номер карты (точное совпадение) | — |
| `status` | string | `APPROVED` / `DECLINED`         | — |
| `dateFrom` | date | С даты                          | — |
| `dateTo` | date | По дату                         | — |
| `merchantId` | string | ID мерчанта                     | — |
| `issuerId` | string | ID эмитента                     | — |
| `mcc` | string | Код категории                   | — |
| `limit` | int | Лимит                           | `50` |
| `offset` | int | Смещение                        | `0` |

Все параметры опциональны, активные объединяются через AND.

**Ответ 200:**
```json
{
  "total": 150,
  "transactions": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "stan": "000001",
      "rrn": "012345678901",
      "pan": "4000000000000001",
      "amount": 150000,
      "currencyCode": "643",
      "status": "APPROVED",
      "authCode": "ABC123",
      "merchantId": "MERCHANT001",
      "mcc": "5411",
      "transmissionDateTime": "2024-01-15T14:30:01Z"
    }
  ]
}
```

> Суммы в копейках (minor units). Для отображения в рублях: `amount / 100`.

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | Невалидные параметры (`limit <= 0`, `offset < 0`) |

---

#### `GET /api/dashboard/stats`

**Ответ 200:**
```json
{
  "totalTransactions": 1250,
  "approvedCount": 1100,
  "declinedCount": 150,
  "approvalRate": 0.88,
  "totalAmount": 187500000,
  "averageAmount": 150000,
  "avgProcessingTimeMs": 38.5,
  "transactionsPerMinute": 12.3
}
```

---

#### `GET /api/dashboard/recent`

**Query-параметры:**

| Параметр | Тип | Описание | По умолчанию |
|----------|-----|----------|--------------|
| `limit` | int | Количество записей | `20` |

**Ответ 200:** массив транзакций, отсортированных по `createdAt DESC`.

---

#### `WS /ws/transactions`

WebSocket-эндпоинт для получения транзакций в реальном времени.

**Подключение:** `ws://localhost:8080/ws/transactions`

Каждая новая транзакция рассылается всем подключённым клиентам в виде JSON-объекта транзакции.

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `8080` | Порт сервиса |
| `POSTGRES_HOST` | `localhost` | Хост PostgreSQL |
| `POSTGRES_PORT` | `5432` | Порт PostgreSQL |
| `POSTGRES_DB` | `processing` | Имя базы данных |
| `POSTGRES_USER` | `postgres` | Пользователь БД |
| `POSTGRES_PASSWORD` | `postgres` | Пароль БД |

---

## Как запустить

### Локально (без Docker)

```bash
./mvnw spring-boot:run
```

### В Docker

```bash
docker build -t transaction-logger .
docker run -p 8080:8080 --env-file ../.env transaction-logger
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d transaction-logger
```

---

## Тестирование

```bash
./mvnw test
```

---

## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем                                                  |
|--------|:-----------:|----------|--------------------------------------------------------|
| Switch | ← входящий | HTTP REST | Присылает транзакции для сохранения                    |
| Gateway | ← входящий | HTTP REST | Проксирует запросы поиска и статистики                 |
| Web Dashboard | ← входящий | WebSocket | Подключается для получения real-time потока транзакций |

---

## Структура проекта

```text
transaction-logger/
├── src/
│   ├── main/
|   |   ├── config/           # Конфигурация
│   │   ├── controller/       # HTTP-контроллеры
│   │   ├── service/          # Бизнес-логика
│   │   ├── repository/       # Доступ к БД
│   │   ├── model/            # JPA-сущности
│   │   ├── dto/              # DTO запросов и ответов
|   |   ├── mapper/           # Маппинг entity <-> DTO
│   │   ├── specification/    # JPA Specification (фильтрация)
│   │   ├── enums/            # Перечисления
│   │   ├── websocket/        # WebSocket-конфигурация и хендлер
│   │   └── exception/        # Обработка ошибок
│   └── test/
├── Dockerfile
└── README.md
```

---

## Авторы

- Капусткин Никита — разработчик Logger #2(Поиск и WebSocket)
- Сидякин Ярослав — разработчик Logger #1(Прием транзакций)

**Группа:** C (Data), A (Core)
