# Проект orbmrkt

## Цель

Разработать микросервисную систему управления заказами и платежами для платформы продажи спутниковых данных. Система обеспечивает асинхронную обработку заказов с гарантированной доставкой событий и корректным управлением балансом геокредитов.

**Ключевые требования:**
- Асинхронная оплата через брокер сообщений (Kafka)
- Transactional Outbox/Inbox для надёжной доставки событий
- Effectively exactly-once списание по order_id
- Корректный баланс при конкурентных операциях
- Единая точка входа через API Gateway

## Стейкхолдеры

| Стейкхолдер | Интерес |
|-------------|---------|
| **Заказчик** (компания-продавец спутниковых данных) | Автоматизация продаж, надёжность платежей, масштабируемость системы |
| **Разработчик** | Качественная архитектура, чистый код, соответствие best practices, демонстрация компетенций |
| **Конечные пользователи** (разработчики фронтенда/мобильных приложений) | Стабильный REST API, понятная документация, предсказуемое поведение системы |

## Roadmap (16.06.2026 — 12.07.2026)

### Неделя 1 (16.06 — 22.06): Планирование и проектирование

**Цель:** Определить архитектуру, спроектировать API, настроить окружение.

- [x] Анализ требований
- [x] Выбор технологического стека (Java 21, Spring Boot 3.5, Kafka, PostgreSQL)
- [x] Архитектура микросервисов (Orders, Payments, API Gateway)
- [x] Проектирование REST API (endpoints, DTO, error codes)
- [x] Настройка Gradle multi-module проекта
- [x] Docker Compose для локальной разработки
- [x] C4 Level 1-2 диаграммы (Context, Container) — [`docs/diagrams/c1-context.puml`](docs/diagrams/c1-context.puml), [`docs/diagrams/c2-container.puml`](docs/diagrams/c2-container.puml) (PDF генерируется локально)

**Результат:** Архитектурное решение, настроенное окружение, базовая структура проекта.

---

### Неделя 2 (23.06 — 29.06): Разработка ядра

**Цель:** Реализовать базовую функциональность сервисов без интеграции.

- [x] Payments Service:
  - [x] Счета (создание, идемпотентность)
  - [x] Баланс (получение, пополнение)
  - [x] Optimistic locking (@Version)
- [x] Orders Service:
  - [x] Создание заказов (валидация payload)
  - [x] Жизненный цикл (CREATED → PAYMENT_PENDING)
  - [x] Статусы (PAID, PAYMENT_FAILED, REJECTED)
- [x] Common DTO (Kafka events, ApiResponse, enums)
- [x] Базовые unit-тесты

**Результат:** Работающие REST API обоих сервисов, готовность к интеграции.

---

### Неделя 3 (30.06 — 06.07): Интеграция

**Цель:** Реализовать асинхронную коммуникацию через Kafka, обеспечить надёжность.

- [x] Kafka интеграция:
  - [x] Producer (Order Service → order.payment.requested)
  - [x] Consumer (Payment Service → обработка оплаты)
  - [x] Producer (Payment Service → order.payment.result)
  - [x] Consumer (Order Service → обновление статуса)
- [x] Transactional Outbox:
  - [x] OutboxEntity, OutboxRepository
  - [x] OutboxPollingWorker (polling, exponential backoff)
- [x] Transactional Inbox:
  - [x] InboxEntity, InboxRepository
  - [x] Дедупликация по event_id
  - [x] Обработка DataIntegrityViolationException
- [x] Идемпотентность списания (ProcessedPaymentEntity по order_id)
- [x] API Gateway:
  - [x] Маршрутизация (Spring Cloud Gateway)
  - [x] Circuit Breaker (Resilience4j)
  - [x] Fallback endpoints
  - [x] Global Error Handler
- [x] Интеграционные тесты:
  - [x] Payment Service (5 сценариев)
  - [x] Order Service (3 сценария)
  - [x] Testcontainers + Embedded Kafka
- [x] Диаграммы потоков данных (Sequence, Activity, State) — [`docs/diagrams/flow-diagrams.md`](docs/diagrams/flow-diagrams.md)

**Результат:** Полностью интегрированная система, все тесты проходят.

---

### Неделя 4 (07.07 — 12.07): Завершение и документация

**Цель:** Подготовить документацию, анализ безопасности, презентацию.

- [x] README.md (архитектура, quick start, API, тестирование)
- [x] docs/analytics.sql (SQL-запросы для статистики)
- [x] C4 диаграммы (Level 1-2) и диаграммы потоков
- [ ] Анализ безопасности:
  - [ ] Идентификация угроз (OWASP Top 10)
  - [ ] Триаж (оценка рисков, mitigation strategies)
  - [ ] Документация SECURITY.md
- [ ] Подготовка презентации:
  - [ ] Слайды (архитектура, технологии, демо)
  - [ ] Демонстрация сценариев (happy path, ошибки)
  - [ ] Q&A подготовка
- [ ] Финальное тестирование (все 8 интеграционных тестов)
- [ ] Защита проекта (12.07.2026)

**Результат:** Готовый проект с полной документацией, успешная защита.

---

## Следующие шаги после защиты (Frontend)

### Этап 5 (после 12.07.2026): Frontend разработка

**Цель:** Реализовать веб-интерфейс для взаимодействия с системой через API Gateway.

**Срок:** 6 недель (1.5 месяца)

**Шаги:**
- [ ] Инициализация React + Vite + TypeScript проекта
- [ ] Настройка code quality (ESLint, Prettier)
- [ ] Разработка API клиента с типизацией (fetch wrapper + TypeScript types)
- [ ] Настройка state management (TanStack Query для async operations)
- [ ] Настройка роутинга (React Router)
- [ ] Создание компонентов:
  - [ ] UserSelector (выбор пользователя)
  - [ ] AccountPanel (создание счета, пополнение, баланс)
  - [ ] OrderPanel (создание заказа, список заказов)
- [ ] Error boundaries и обработка ошибок UI
- [ ] Интеграция с API Gateway через Vite proxy
- [ ] Polling статуса заказа (с автоматическим retry)
- [ ] UI библиотека и стилизация (Tailwind CSS / Material-UI)
- [ ] Интеграционное тестирование (Playwright / Cypress)
- [ ] Оптимизация производительности (lazy loading, code splitting)

**Результат:** Полноценное веб-приложение для управления заказами и платежами с современным UI.

---

## Текущий статус (06.07.2026)

**Прогресс:** 85%

### Выполнено:
- ✅ Архитектура и проектирование
- ✅ Payments Service (полная функциональность)
- ✅ Orders Service (полная функциональность)
- ✅ Kafka интеграция (Outbox/Inbox)
- ✅ API Gateway (маршрутизация, Circuit Breaker)
- ✅ Интеграционные тесты (8/8 проходят)
- ✅ README.md
- ✅ C4 диаграммы (Level 1-2)
- ✅ Диаграммы потоков (Sequence, Activity, State)

### Осталось:
- ⏳ Анализ безопасности (триаж)
- ⏳ Подготовка презентации

## Технологический стек

| Компонент | Технология | Версия |
|-----------|-----------|--------|
| Язык | Java | 21 |
| Фреймворк | Spring Boot | 3.5.13 |
| Сборка | Gradle (Kotlin DSL) | 9 |
| База данных | PostgreSQL | 16 |
| Брокер сообщений | Apache Kafka (KRaft) | latest |
| API Gateway | Spring Cloud Gateway | WebFlux |
| Circuit Breaker | Resilience4j | - |
| Тестирование | JUnit 5, Testcontainers, Embedded Kafka | - |
| Контейнеризация | Docker, docker-compose | - |

## Архитектура

```
┌─────────────────┐
│   API Gateway   │ :8080
│  (Spring Cloud) │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌──────────┐
│ Orders │ │ Payments │
│ :8081  │ │  :8082   │
└───┬────┘ └────┬─────┘
    │           │
    └─────┬─────┘
          │
    ┌─────▼─────┐
    │   Kafka   │
    │  (KRaft)  │
    └───────────┘
```

**Event Flow:**
1. Client → API Gateway → Orders Service (POST /orders)
2. Orders Service → Kafka (order.payment.requested)
3. Kafka → Payments Service (debit)
4. Payments Service → Kafka (order.payment.result)
5. Kafka → Orders Service (update status)

## Критерии приёмки

- [ ] Все 5 сценариев из чек-листа проходят
- [ ] 8 интеграционных тестов проходят
- [ ] C4 диаграммы (Level 1-4) готовы
- [ ] Анализ безопасности завершён
- [ ] Документация полная (README, PROJECT, SECURITY)
- [ ] Демо работает без ошибок
- [ ] Защита успешно проведена
