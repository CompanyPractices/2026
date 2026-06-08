# Web-Dashboard

## Назначение

> Web Dashboard — это пользовательский интерфейс процессингового центра, который отображает ключевые метрики транзакций (KPI-карточки), графики динамики и таблицу последних операций в режиме реального времени.

---

## Технологии

- **Язык:** TypeScript 5
- **Фреймворк:** React 18 + Vite
- **Стилизация:** Tailwind CSS
- **База данных:** нет
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check |
| GET | `/` | Главная страница Dashboard (SPA) |

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "UP",
  "service": "dashboard",
  "version": "1.0.0"
}
```

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `VITE_API_URL` | `http://localhost:8080` | URL API Gateway для HTTP-запросов |
| `VITE_WS_URL` | `ws://localhost:8088` | URL Transaction Logger для WebSocket |


---

## Как запустить

### Локально (без Docker)

```bash
npm install
npm run dev
```

### В Docker

```bash
docker build -t dashboard .
docker run -p 3000:3000 --env-file ../.env dashboard
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d dashboard
```

---

## Тестирование

```bash
# Unit-тесты
npm run test

# Линтер
npm run lint
```

---

## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем |
|--------|:----------:|----------|-------|
| Gateway | → исходящий | HTTP REST | Получение статистики (`/api/dashboard/stats`) и транзакций (`/api/dashboard/recent`, `/api/transactions/search`) |
| Transaction Logger | → исходящий | WebSocket | Получение потока транзакций в реальном времени (`/ws/transactions`) |

---

## Структура проекта

```text
dashboard/
├── src/
│   ├── api/                       # API-клиент
│   ├── components/                # React-компоненты
│   ├── hooks/                     # Кастомные хуки
│   ├── types/                     # TypeScript-типы
│   ├── utils/                     # Вспомогательные функции
│   ├── mockData.ts                # Моковые данные для разработки
│   ├── App.tsx                    # Корневой компонент
│   ├── App.test.tsx               # Тесты корневого компонента
│   └── main.tsx                   # Точка входа
├── nginx.conf                     # Конфигурация Nginx (раздача статики + /health)
├── Dockerfile                     # Multi-stage build (Node → Nginx)
├── vite.config.ts                 # Конфигурация Vite
├── package.json
└── README.md
```

---

---

## Авторы

- Алина Рамазанова — разработчик
- Юля Кулакова — разработчик

**Группа:** B (Gateway & Frontend)
