# Card Management

## Назначение

Card Management создает карты, управляет балансом, проверяет наличие карты в бд.

---

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** PostgreSQL
- **Контейнеризация:** Docker

---

## Endpoints

| Метод  | Путь                  | Описание                              |
|--------|-----------------------|---------------------------------------|
| GET    | `/health`             | Health-check сервиса                  |
| POST   | `/api/cards`          | Создать карту                         |
| GET    | `/api/cards/{PAN}`    | Получить карту по номеру              |
| GET    | `/api/cards/`         | Получить карты по фильтрам            |
| PATCH  | `/api/cards/{PAN}`    | Изменить информацию о карте           |
| DELETE | `/api/cards/{PAN}`    | Удалить карту по номеру (soft delete) |
| POST   | `/api/cards/generate` | Сгенерировать N карт                  |

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "ok",
  "service": "card-management",
  "cardsInDatabase": 100500
}
```

#### `POST /api/cards`

**Тело запроса:**
```json
{
  "bin": "400000",
  "cardholderName": "TYLER DURDEN",
  "concurrencyCode": "683",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "initialBalance": 1000000
}
```

**Ответ 201:**
```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1234",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

#### `GET /api/cards/{PAN}`

**Ответ 200:**
```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1970-01-01",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

#### `GET /api/cards/`

**Тело запроса** (все данные опциональны)
```json
{
  "limit": 10,
  "offset": 0,
  "status": "ACTIVE",
  "bin": "123456",
  "issuerId": "ZZZZZZ",
  "startDate": "1970-01-01T00:00:00",
  "endDate": "2077-01-01T00:00:00"
}
```

**Ответ 200:**
```json
[
  {
    "id": "d3b07384-d113-44a6-a070-658b446a81d4",
    "pan": "1234567891011121",
    "bin": "123456",
    "cardholderName": "TYLER DURDEN",
    "expiryDate": "1970-01-01",
    "status": "ACTIVE",
    "dailyLimit": 15000000,
    "monthlyLimit": 300000000,
    "availableBalance": 1000000,
    "issuerId": "ZZZZZZ",
    "createdAt": "1970-01-01T00:00:00"
  },
  // ...
]
```
#### `PATCH /api/cards/{pan}`

**Тело запроса** (все данные опциональны)
```json
{
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000
}
```

**Ответ 204 (no content)**

#### `DELETE /api/cards/{pan}`
**Ответ 204 (no content)**

#### `POST /api/cards/generate`

**Тело запроса**
```json
{
  "count": 100,
  "bin": [123456, 222333, 400000] // not empty
}
```

**Ответ 201**
```json
{
  "generated": 100,
  "cards": [
    {
      "id": "d3b07384-d113-44a6-a070-658b446a81d4",
      "pan": "1234567891011121",
      "bin": "123456",
      "cardholderName": "TYLER DURDEN",
      "expiryDate": "1970-01-01",
      "status": "ACTIVE",
      "dailyLimit": 15000000,
      "monthlyLimit": 300000000,
      "availableBalance": 1000000,
      "issuerId": "ZZZZZZ",
      "createdAt": "1970-01-01T00:00:00"
    },
    // ...
  ]
}
```

---

**Ошибки:**

| Код | Условие              |
|-----|----------------------|
| 400 | Ошибка валидации     |
| 402 | Недостаточно средств |
| 404 | Карта не найдена     |
| 500 | Неизвестная ошибка   |

**Пример**
```json
{
  "status": 404,
  "error": "com.processing.exceptions.CardNotFoundException",
  "message": "Card with present PAN was not found",
  "timestamp": "1970-01-01T00:00:00"
}
```

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная    | Значение по умолчанию | Описание             |
|---------------|-----------------------|----------------------|
| `SERVER_PORT` | `8080`                | Порт сервиса         |
| `DB_HOST`     | `localhost`           | Хост PostgreSQL      |
| `DB_PORT`     | `5432`                | Порт PostgreSQL      |
| `DB_NAME`     | `postgres`            | Имя базы данных      |
| `DB_USER`     | `postgres`            | Пользователь БД      |
| `DB_PASSWORD` | `postgres`            | Пароль БД            |

---

## Как запустить

### Локально

```bash
docker compose -f docker-compose.local.yml up -d # Если нужен Postgres
mvn spring-boot:run
```

### В Docker

```bash
docker build -t card-management .
docker run -p 8083:8080 --env-file ../.env card-management
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d card-management
```

---

## Тестирование

```bash
mvn test
```

---

## Взаимодействие с другими сервисами

| Сервис                | Направление | Протокол  | Зачем                                             |
|-----------------------|:-----------:|-----------|---------------------------------------------------|
| Authorization Service | ← входящий  | HTTP REST | Валидация, создание карт, работа с балансом счета |

---

## Структура проекта

```text
card-management/
├── src/
│   ├── main/
│   │   ├── annotations/    # Кастомные аннотации
│   │   ├── configuration/  # Конфигурационные файлы Spring
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── exceptions/     # Бизнес-исключения
│   │   ├── models/         # Модели данных / DTO
│   │   ├── options/        # Настройки для компонентов
│   │   ├── repositories/   # Доступ к БД
│   │   ├── services/       # Бизнес-логика
│   └── test/               # Тесты
├── Dockerfile
└── README.md
```

---

## Авторы

- Владимир Рубахин — разработчик
- Даниил Станкевич — разработчик

**Группа:** C (Data & Simulators)
