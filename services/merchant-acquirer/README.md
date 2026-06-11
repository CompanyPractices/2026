# Merchant acquirer simulator

## Назначение

Сервис отвечает за генерацию транзакций, которые отправляет на авторизацию в API Gateway. В отличие от Terminal Simulator, данный сервис генерирует правдоподобные транзакции на основе множества мерчантов и экваеров.

---

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** PostgreSQL
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь                          | Описание                              |
|-------|-------------------------------|---------------------------------------|
| GET   | `/health`                     | Health-check сервиса                  |
| GET   | `/api/simulator/merchants`    | Получение доступных мерчантов         |
| POST  | `/api/simulator/merchant/run` | Запуск симулятора отправки транзакций |

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

#### `GET /api/simulator/merchants`

**Ответ 200:**
```json
{
  "id": "MERCH00000000005",
  "name": "Перекрёсток #042",
  "mcc": "5411",
  "category": "grocery",
  "acquirerId": "ACQ001",
  "acquiringFee": 0,
  "averageCheck": 120000
}
```

#### `POST /api/simulator/merchant/run`

**Тело запроса:**
```json
{
  "count": "value",
  "mccCodes": "massive",
  "scenario": "enum: grocery, electronics, restaurant, travel"
}
```

**Ответ 200:**
```json
{
  "totalSubmitted": 50,
  "approved": 47,
  "declined": 3,
  "elapsedMs": 2300,
  "transactions": ["AuthorizationResponse"]
}
```

#### `GET /api/simulator/merchant/fee`

**Тело запроса:**
```json
{
  "transmissionDateTime": "dataTime",
  "stan": "string",
  "pan": "string",
  "terminalId": "string",
  "amount": "long"
}
```

**Ответ 200:**
```json
{
  "acquirerFee": "long"
}
```

**Ошибки: (TODO)**
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | {Сущность} не найдена |
| 503 | Downstream-сервис недоступен |

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная          | Значение по умолчанию | Описание |
|---------------------|----------------------|----------|
| `PORT`              | `{8080}` | Порт сервиса |
| `POSTGRES_HOST`     | `postgres` | Хост PostgreSQL |
| `POSTGRES_PORT`     | `5432` | Порт PostgreSQL |
| `POSTGRES_DB`       | `smp_db` | Имя базы данных |
| `POSTGRES_USER`     | `smp_user` | Пользователь БД |
| `POSTGRES_PASSWORD` | `smp_password` | Пароль БД |
| `GATEWAY_URL`       | `http://localhost:{port}` | URL смежного сервиса |

---

## Как запустить

### Локально (без Docker)

```bash
./mvnw spring-boot:run
```

### В Docker

```bash
docker build -t merchant-aqcuirer .
docker run -p {port}:{port} --env-file ../.env merchant-aqcuirer
```

### В составе Docker Compose

```bash
docker compose up -d merchant-aqcuirer
```

---

## Тестирование

```bash
./mvnw test
```

---

## Взаимодействие с другими сервисами

| Сервис      | Направление | Протокол | Зачем                                                     |
|-------------|:----------:|----------|-----------------------------------------------------------|
| API Gateway | → исходящий | HTTP REST | Вызов API Gateway для отправки транзакции                 |
| API Gateway | ← входящий | HTTP REST | API Gateway возвращает результаты транзакций              |
| API Gateway | ← входящий | HTTP REST | Внутренний сервис обращается за получение комиссии экваера |
| API Gateway | → исходящий | HTTP REST | Возвращаю комиссию экэваера                               |

---

## Структура проекта

```text
merchant-aqcuirer/
├── src/
│   ├── main/
│   │   ├── client/         #Клиент для отправки HTTP запросов
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── domain/         # Доменные сущности
│   │   │   ├── entity/
│   │   │   ├── factory/
│   │   │   └── model/
│   │   ├── repositories/   # Доступ к БД
│   │   ├── services/       # Бизнес-логика
│   └── test/               # Тесты
├── Dockerfile
├── README.md
└── .env
```

---

## Авторы

- Козлов Богдан — разработчик

**Группа:** C (Data & Simulators)
