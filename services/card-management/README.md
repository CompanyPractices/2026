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

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check сервиса |
| POST | `/api/...` | {Краткое описание} |

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

#### `POST /api/...`

**Тело запроса:**
```json
{
  "field1": "value",
  "field2": 123
}
```

**Ответ 200:**
```json
{
  "result": "ok",
  "data": {}
}
```

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | {Сущность} не найдена |

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `8080`               | Порт сервиса |
| `DB_HOST` | `localhost`          | Хост PostgreSQL |
| `DB_PORT` | `5432`               | Порт PostgreSQL |
| `DB_NAME` | `postgres`           | Имя базы данных |
| `DB_USER` | `postgres`           | Пользователь БД |
| `DB_PASSWORD` | `postgres`           | Пароль БД |

---

## Как запустить

### Локально (без Docker)

```bash
./mvnw spring-boot:run
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
./mvnw test
```

---

## Взаимодействие с другими сервисами

| Сервис                | Направление | Протокол | Зачем                                             |
|-----------------------|:----------:|----------|---------------------------------------------------|
| Authorization Service | ← входящий | HTTP REST | Валидация, создание карт, работа с балансом счета |

---

## Структура проекта

```text
card-management/
├── src/
│   ├── main/
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── services/       # Бизнес-логика
│   │   ├── models/         # Модели данных / DTO
│   │   ├── repositories/   # Доступ к БД
│   └── test/               # Тесты
├── Dockerfile
└── README.md
```

---

## Авторы

- Владимир Рубахин — разработчик
- Даниил Станкевич — разработчик

**Группа:** C (Data & Simulators)
