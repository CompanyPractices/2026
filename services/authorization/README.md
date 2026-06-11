# Authorization
## Назначение
Сервис авторизации — принимает решение одобрить или отклонить транзакцию
Пример:
> Authorization Service проверяет каждую транзакцию: статус карты, лимиты, баланс — и возвращает решение APPROVED или DECLINED.
---
## Технологии
- Язык: {Java 21}
- Фреймворк: {Spring Boot 3}
- База данных: {PostgreSQL}
- Контейнеризация: Docker
---
## Endpoints
| Метод | Путь | Описание |
|-------|------|----------|
| GET | /health | Health-check сервиса |
| POST | /api/internal/authorize | Authorization: проверка |
### Подробно
#### GET /health
Ответ 200:
```
{
  "status": "ok",
  "service": "authservice",
  "dependencies": {
    "{dep1}": "ok"
  }
}
```
#### POST /api/internal/authorize
Тело запроса:
```
{
  "mti": "0100",
  "stan": "123456",
  "pan": "1234567890123456",
  "processingCode": "000000",
  "amount": 0,
  "currencyCode": "810",
  "transmissionDateTime": "2026-06-05T18:12:49.07",
  "terminalId": "T0000001",
  "terminalType": "ATM",
  "merchantId": "M00000000000001",
  "mcc": "5411",
  "acquirerId": "A001",
  "issuerId": "I001"
}
```
Ответ 200:
```
{
  "mti": "0100",
  "stan": "123456",
  "rrn": "616211293600"
  "authCode": "A1B2C3",
  "responseCode": "00",
  "status": "APPROVED",
  "declineReason": "",
  "processingTimeMs": 42
}
```
Ошибки:
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 403 | Карта заблокирована, неактивна или просрочена |
| 404 | Карта не найдена |
| 422 | Недостаточно средств |
| 503 | Downstream-сервис недоступен |
---
## Конфигурация
Все параметры через переменные окружения (файл .env в корне проекта):
| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| PORT | {8083} | Порт сервиса |
| DB_HOST | localhost | Хост PostgreSQL |
| DB_PORT | 5432 | Порт PostgreSQL |
| DB_NAME | smp_db | Имя базы данных |
| DB_USER | smp_user | Пользователь БД |
| DB_PASSWORD | smp_password | Пароль БД |
| CARD_MGMT_URL | http://localhost:8081 | URL смежного сервиса |
---
## Как запустить
### Локально (без Docker)
```
# Java (Spring Boot)
mvn spring-boot:run
```
### В Docker
```
docker build -t authorization .
docker run -p 8083:8080 --env-file ../.env authorization
```
### В составе Docker Compose
```
# Из корня репозитория
docker compose up -d authorization
```
---
## Тестирование
```
# Java
mvn test
```
---
## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем |
|--------|:----------:|----------|-------|
| Switch | ← входящий | HTTP REST | присылает запросы на вход |
| CardManagment | → исходящий | HTTP REST | запрашиваем данные карты и резервирум средства |

---
## Структура проекта
```text
authorization/src/
├── main/
│   └── java.com.processing.authorization/
│       ├── configs/
│       ├── constants/      # HTTP-handlers / controllers
│       ├── controller/     # HTTP-handlers / controllers
│       ├── dto/            # Модели данных / DTO
│       ├── entities/
│       ├── exceptions/
│       ├── repositories/
│       └── services/
├── test/                   # Тесты
│   ├── resoursces/
│   └── java.com.processing.authorization/
│       ├── controller/
│       ├── integration/
│       └── service/
├── Dockerfile
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
└── {специфичные файлы}
```
---
## Авторы
- Дарья Ермолаева — разработчик
- Алина Клименко — разработчик
Группа: A { Core }
