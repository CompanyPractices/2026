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
| GET | `/api/cards/{pan}` | Получить карту по PAN |
| POST | `/api/internal/authorize` | Authorization: проверка |
| POST | `/api/cards/{pan}/reserve` | Резервирует сумму с availableBalance карты. |

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "ok",
  "service": "{service-name}",
  "dependencies": {
    "{dep1}": "ok"
  }
}
```

#### `GET /api/cards/{pan}`

get card 

#### `POST /api/internal/authorize`

**Тело запроса:**
```json
{
  "mti": "value",
  "stan": "value",
  "rrn": "",
  "pan": "",
  "processingCode": "",
  "amount": 0,
  "currencyCode": "",
  "transmissionDateTime": "2026-06-03T12:00:00",
  "terminalId": "",
  "terminalType": "ATM",
  "merchantId": "",
  "mcc": "",
  "acquirerId": "",
  "issuerId": ""
}
```

**Ответ 200:**
```json
{
  "mti": "0110",
  "stan": "",
  "rrn": "",
  "authCode": "",
  "responseCode": "",
  "status": "",
  "declineReason": "",
  "processingTimeMs": 0
}
```

**Ошибки:**
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
| `PORT` | `{8080}` | Порт сервиса |
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
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── dto/          # Модели данных / DTO
│   │   ├── enums/   
│   └── test/               # Тесты
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