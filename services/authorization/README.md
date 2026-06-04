# Authorization

## Назначение

Сервис авторизации — принимает решение одобрить или отклонить транзакцию

Пример:
> Authorization Service проверяет каждую транзакцию: статус карты, лимиты, баланс — и возвращает решение APPROVED или DECLINED.

---

## Технологии

- **Язык:** {Java 21}
- **Фреймворк:** {Spring Boot 3}
- **База данных:** {PostgreSQL}
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check сервиса |
| POST | `/api/internal/authorize` | Authorization: проверка |
| POST | `/api/cards/{pan}/reserve` | Резервирует сумму с availableBalance карты. |

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "ok",
  "service": "authorization",
  "dependencies": {
    "cardManagement": "ok"
  }
}
```

#### `POST /api/internal/authorize`

**Тело запроса:**
```json
{
  "mti": "0100",
  "stan": "000001",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 150000,
  "currencyCode": "643",
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "terminalId": "TERM001",
  "terminalType": "POS",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001",
  "issuerId": "ISS001"
}
```

**Ответ 200:**
```json
{
  "mti": "0110",
  "stan": "000001",
  "rrn": "012345678901",
  "authCode": "ABC123",
  "responseCode": "00",
  "status": "APPROVED",
  "declineReason": "",
  "processingTimeMs": 42
}
```

**Ошибки:** //TODO
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | {Сущность} не найдена |
| 503 | Downstream-сервис недоступен |

#### `POST /api/cards/{pan}/reserve`

**Тело запроса:**
```json
{
  "amount": 150000,
  "rrn": "012345678901"
}
```

**Ответ 200:**
```json
true
```

**Ошибки:** //TODO
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | {Сущность} не найдена |
| 503 | Downstream-сервис недоступен |

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `{8083}` | Порт сервиса |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `processing` | Имя базы данных |
| `DB_USER` | `postgres` | Пользователь БД |
| `DB_PASSWORD` | `postgres` | Пароль БД |
| `{UPSTREAM}_URL` | `http://localhost:{port}` | URL смежного сервиса |

---

## Как запустить

### Локально (без Docker)

```bash
# Java (Spring Boot)
./mvnw spring-boot:run
```

### В Docker

```bash
docker build -t {service-name} .
docker run -p {port}:{port} --env-file ../.env {service-name}
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d {service-name}
```

---

## Тестирование

```bash
# Java
./mvnw test
```

---

## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем |
|--------|:----------:|----------|-------|
| Gateway | ← входящий | HTTP REST | присылает запросы на вход |
| CardManagment | → исходящий | HTTP REST | запрашиваем данные карты и резервирум средства |

---

## Структура проекта

```text
{authorization}/
├── src/main
│   ├── java.com.processing/
│   │   ├── controller/             # HTTP-handlers / controllers
│   │   ├── dto/                    # Модели данных / DTO
│   │   ├── enums/   
│   │   └── Application.java        # Точка входа
│   ├── resourses/ 
│   │   └── application.properties
│   └── test/                       # Тесты
├── Dockerfile
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
└── {специфичные файлы}
```

---

## Авторы

- {Дарья Ермолаева} — разработчик
- {Алина Клименко} — разработчик

**Группа:** A { Core }