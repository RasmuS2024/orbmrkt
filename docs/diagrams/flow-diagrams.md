# Диаграммы потоков (Flow Diagrams)

## Описание

Диаграммы потоков показывают взаимодействие между компонентами системы во времени (Sequence Diagrams) и жизненный цикл сущностей (State Diagrams).

---

## 1. Sequence Diagram: Happy Path (успешная оплата)

### Описание
Пользователь создаёт заказ, система успешно списывает средства и обновляет статус заказа на PAID.

### Диаграмма

```mermaid
sequenceDiagram
    autonumber
    actor User as Пользователь
    participant GW as API Gateway
    participant OS as Order Service
    participant ODB as Order DB
    participant K as Kafka
    participant PS as Payment Service
    participant PDB as Payment DB

    User->>GW: POST /api/v1/orders<br/>{product_type, price, payload}
    GW->>OS: Маршрутизация + X-User-Id
    OS->>ODB: INSERT orders (status=CREATED)
    OS->>ODB: INSERT outbox (OrderPaymentRequested)
    OS->>ODB: UPDATE orders (status=PAYMENT_PENDING)
    OS-->>GW: 201 Created {order_id, status}
    GW-->>User: Response

    Note over OS,K: OutboxPollingWorker (каждые 500ms)
    OS->>ODB: SELECT * FROM outbox WHERE processed=false
    OS->>K: kafkaTemplate.send(order.payment.requested)
    OS->>ODB: UPDATE outbox SET processed=true

    K->>PS: Consume OrderPaymentRequested
    PS->>PDB: SELECT inbox WHERE event_id=?
    PS->>PDB: INSERT inbox (event_id)
    PS->>PDB: SELECT accounts WHERE user_id=?
    PS->>PDB: UPDATE accounts SET balance=balance-amount<br/>(optimistic locking @Version)
    PS->>PDB: INSERT processed_payments (order_id)
    PS->>PDB: INSERT outbox (OrderPaymentCompleted)

    Note over PS,K: OutboxPollingWorker (каждые 500ms)
    PS->>PDB: SELECT * FROM outbox WHERE processed=false
    PS->>K: kafkaTemplate.send(order.payment.result)
    PS->>PDB: UPDATE outbox SET processed=true

    K->>OS: Consume OrderPaymentCompleted
    OS->>ODB: SELECT inbox WHERE event_id=?
    OS->>ODB: INSERT inbox (event_id)
    OS->>ODB: UPDATE orders SET status=PAID

    Note over User,OS: Пользователь может запросить статус
    User->>GW: GET /api/v1/orders/{order_id}
    GW->>OS: Маршрутизация
    OS->>ODB: SELECT orders WHERE id=?
    OS-->>GW: 200 OK {status=PAID}
    GW-->>User: Response
```

### Ключевые моменты
- **Шаги 1-6:** Синхронное создание заказа (одна транзакция)
- **Шаги 7-9:** Асинхронная публикация события через Outbox
- **Шаги 10-16:** Обработка оплаты в Payment Service (одна транзакция)
- **Шаги 17-19:** Асинхронная публикация результата через Outbox
- **Шаги 20-23:** Обновление статуса заказа в Order Service

---

## 2. Sequence Diagram: Payment Failed (недостаточно средств)

### Описание
Пользователь создаёт заказ, но на счету недостаточно средств. Система отклоняет оплату и устанавливает статус PAYMENT_FAILED.

### Диаграмма

```mermaid
sequenceDiagram
    autonumber
    actor User as Пользователь
    participant GW as API Gateway
    participant OS as Order Service
    participant ODB as Order DB
    participant K as Kafka
    participant PS as Payment Service
    participant PDB as Payment DB

    User->>GW: POST /api/v1/orders<br/>{product_type, price, payload}
    GW->>OS: Маршрутизация + X-User-Id
    OS->>ODB: INSERT orders (status=CREATED)
    OS->>ODB: INSERT outbox (OrderPaymentRequested)
    OS->>ODB: UPDATE orders (status=PAYMENT_PENDING)
    OS-->>GW: 201 Created {order_id, status}
    GW-->>User: Response

    Note over OS,K: OutboxPollingWorker
    OS->>K: Publish OrderPaymentRequested

    K->>PS: Consume OrderPaymentRequested
    PS->>PDB: INSERT inbox (event_id)
    PS->>PDB: SELECT accounts WHERE user_id=?
    PS->>PDB: Проверка: balance < amount
    PS->>PDB: INSERT outbox (OrderPaymentFailed, reason=INSUFFICIENT_BALANCE)

    Note over PS,K: OutboxPollingWorker
    PS->>K: Publish OrderPaymentFailed

    K->>OS: Consume OrderPaymentFailed
    OS->>ODB: INSERT inbox (event_id)
    OS->>ODB: UPDATE orders SET status=PAYMENT_FAILED,<br/>failure_reason=INSUFFICIENT_BALANCE

    User->>GW: GET /api/v1/orders/{order_id}
    GW->>OS: Маршрутизация
    OS-->>GW: 200 OK {status=PAYMENT_FAILED, failure_reason}
    GW-->>User: Response
```

### Ключевые моменты
- **Шаг 11:** Проверка баланса выявляет недостаточно средств
- **Шаг 12:** Публикация события `OrderPaymentFailed` с причиной
- **Шаг 17:** Статус заказа обновляется на `PAYMENT_FAILED`
- Баланс остаётся неизменным, средства не списываются

---

## 3. Sequence Diagram: Outbox/Inbox Pattern (детально)

### Описание
Детальное взаимодействие компонентов Transactional Outbox и Inbox для обеспечения надёжной доставки событий.

### Диаграмма

```mermaid
sequenceDiagram
    autonumber
    participant Service as Сервис (Order/Payment)
    participant DB as База данных
    participant Worker as OutboxPollingWorker
    participant Kafka as Kafka Broker
    participant Consumer as Kafka Consumer

    Note over Service,DB: === OUTBOX: Публикация события ===

    Service->>DB: BEGIN TRANSACTION
    Service->>DB: INSERT business_entity (заказ/счёт)
    Service->>DB: INSERT outbox (event_id, topic, payload)
    Service->>DB: COMMIT TRANSACTION

    Note over Service,DB: Событие сохранено в БД, но ещё не отправлено в Kafka

    loop Every 500ms
        Worker->>DB: SELECT * FROM outbox<br/>WHERE processed=false<br/>AND attempts < max_attempts<br/>AND (next_retry_at IS NULL OR next_retry_at <= NOW())<br/>LIMIT batch_size
        DB-->>Worker: Список неотправленных событий

        loop Для каждого события
            Worker->>Kafka: kafkaTemplate.send(topic, key, payload).get()
            alt Успешная отправка
                Kafka-->>Worker: ACK
                Worker->>DB: UPDATE outbox SET processed=true
            else Ошибка отправки
                Kafka-->>Worker: Exception
                Worker->>DB: UPDATE outbox SET attempts=attempts+1,<br/>last_error=?,<br/>next_retry_at=NOW() + 2^attempts seconds
            end
        end
    end

    Note over Kafka,Consumer: === INBOX: Потребление события ===

    Kafka->>Consumer: Poll (получение сообщения)
    Consumer->>DB: SELECT inbox WHERE event_id=?
    
    alt Событие уже обработано (дубликат)
        DB-->>Consumer: EXISTS
        Consumer->>Kafka: ACK (пропуск обработки)
    else Новое событие
        DB-->>Consumer: NOT EXISTS
        Consumer->>DB: BEGIN TRANSACTION
        Consumer->>DB: INSERT inbox (event_id, processed_at)
        
        alt Concurrent insert (гонка)
            DB-->>Consumer: DataIntegrityViolationException (PK violation)
            Consumer->>Kafka: ACK (пропуск обработки)
        else Успешная вставка
            Consumer->>DB: Обработка бизнес-логики
            Consumer->>DB: COMMIT TRANSACTION
            Consumer->>Kafka: ACK
        end
    end

    Note over Worker,DB: === CLEANUP: Очистка старых записей ===

    loop Every day at 03:00
        Worker->>DB: DELETE FROM outbox<br/>WHERE processed=true<br/>AND created_at < NOW() - cleanup_days
        Worker->>DB: DELETE FROM inbox<br/>WHERE processed_at < NOW() - cleanup_days
    end
```

### Ключевые моменты

**Outbox:**
- **Шаги 1-4:** Атомарное сохранение бизнес-сущности и события в одной транзакции
- **Шаги 6-20:** Polling worker асинхронно публикует события в Kafka
- **Экспоненциальная задержка:** 2s → 4s → 8s → 16s → 32s при ошибках
- **Dead letter:** После `max_attempts` событие остаётся в БД с `processed=false`

**Inbox:**
- **Шаги 22-38:** Consumer проверяет дубликаты перед обработкой
- **Двойная защита:** `existsByEventId()` + catch `DataIntegrityViolationException`
- **Idempotency:** Дубликаты пропускаются без retry

**Cleanup:**
- **Шаги 40-43:** Ежедневная очистка старых записей (по умолчанию 7 дней)

---

## 4. State Diagram: Order Lifecycle

### Описание
Жизненный цикл заказа от создания до финального статуса.

### Диаграмма

```mermaid
stateDiagram-v2
    [*] --> CREATED: POST /orders

    CREATED --> PAYMENT_PENDING: OrderPaymentRequested опубликовано
    CREATED --> REJECTED: Ошибка валидации

    PAYMENT_PENDING --> PAID: OrderPaymentCompleted получено
    PAYMENT_PENDING --> PAYMENT_FAILED: OrderPaymentFailed получено

    REJECTED --> [*]
    PAID --> [*]
    PAYMENT_FAILED --> [*]

    note right of CREATED
        Валидация → Сохранение (OK)
        Валидация → Ошибка (невалидный payload/price/product_type)
    end note

    note right of REJECTED
        failure_reason: UNKNOWN_PRODUCT_TYPE
        failure_reason: INVALID_PRICE
        failure_reason: INVALID_PAYLOAD
    end note

    note right of PAYMENT_FAILED
        failure_reason: INSUFFICIENT_BALANCE
        failure_reason: ACCOUNT_NOT_FOUND
        failure_reason: INTERNAL_ERROR
    end note
```

### Статусы

| Статус | Описание | Переход |
|--------|----------|---------|
| **CREATED** | Заказ создан, событие на оплату ещё не отправлено | Начальный статус после POST /orders |
| **PAYMENT_PENDING** | Команда на списание отправлена в Kafka | После публикации OrderPaymentRequested |
| **PAID** | Списание успешно | После получения OrderPaymentCompleted |
| **PAYMENT_FAILED** | Оплата не прошла | После получения OrderPaymentFailed |
| **REJECTED** | Заказ отклонён при создании | При ошибке валидации (невалидный payload, price ≤ 0, неизвестный product_type) |

### Причины отказа (failure_reason)

| Код | Описание | Когда возникает |
|-----|----------|-----------------|
| **UNKNOWN_PRODUCT_TYPE** | Неподдерживаемый тип продукта | product_type не ARCHIVE/TASKING/MONITORING |
| **INVALID_PRICE** | Некорректная цена | price ≤ 0 или null |
| **INVALID_PAYLOAD** | Невалидный payload | Отсутствуют обязательные поля (aoi, capture_date, sensor_type для ARCHIVE) |
| **INSUFFICIENT_BALANCE** | Недостаточно средств | balance < amount |
| **ACCOUNT_NOT_FOUND** | Счёт не найден | Пользователь не создал счёт |
| **INTERNAL_ERROR** | Внутренняя ошибка | Непредвиденная ошибка при обработке |

---

## 5. Activity Diagram: Процесс создания и оплаты заказа

### Описание
Бизнес-процесс от создания заказа до финального статуса с ветвлениями и циклами.

### Диаграмма

```mermaid
flowchart TD
    Start([Начало]) --> ValidateUser{X-User-Id<br/>присутствует?}
    
    ValidateUser -->|Нет| Error1[400 MISSING_USER_ID]
    Error1 --> End1([Конец])
    
    ValidateUser -->|Да| ValidateProduct{product_type<br/>валиден?}
    
    ValidateProduct -->|Нет| Reject1[CREATED → REJECTED<br/>failure_reason=UNKNOWN_PRODUCT_TYPE]
    Reject1 --> End2([Конец])
    
    ValidateProduct -->|Да| ValidatePrice{price > 0?}
    
    ValidatePrice -->|Нет| Reject2[CREATED → REJECTED<br/>failure_reason=INVALID_PRICE]
    Reject2 --> End3([Конец])
    
    ValidatePrice -->|Да| ValidatePayload{payload<br/>валиден?}
    
    ValidatePayload -->|Нет| Reject3[CREATED → REJECTED<br/>failure_reason=INVALID_PAYLOAD]
    Reject3 --> End4([Конец])
    
    ValidatePayload -->|Да| CreateOrder[INSERT orders<br/>status=CREATED]
    
    CreateOrder --> SaveOutbox[INSERT outbox<br/>OrderPaymentRequested]
    
    SaveOutbox --> UpdateStatus[UPDATE orders<br/>status=PAYMENT_PENDING]
    
    UpdateStatus --> ReturnResponse["201 Created<br/>{order_id, status}"]
    
    ReturnResponse --> WaitOutbox[Ожидание OutboxPollingWorker]
    
    WaitOutbox --> PublishKafka[Publish to Kafka<br/>order.payment.requested]
    
    PublishKafka --> WaitPayment[Ожидание обработки Payment Service]
    
    WaitPayment --> CheckBalance{balance >= amount?}
    
    CheckBalance -->|Нет| PublishFailed[Publish OrderPaymentFailed<br/>reason=INSUFFICIENT_BALANCE]
    
    CheckBalance -->|Да| DebitBalance[UPDATE accounts<br/>balance = balance - amount<br/>@Version optimistic locking]
    
    DebitBalance --> SaveProcessed[INSERT processed_payments<br/>order_id]
    
    SaveProcessed --> PublishCompleted[Publish OrderPaymentCompleted<br/>new_balance]
    
    PublishFailed --> UpdateOrderFailed[UPDATE orders<br/>status=PAYMENT_FAILED<br/>failure_reason=INSUFFICIENT_BALANCE]
    
    PublishCompleted --> UpdateOrderPaid[UPDATE orders<br/>status=PAID]
    
    UpdateOrderFailed --> End5([Конец])
    UpdateOrderPaid --> End6([Конец])
```

### Ветвления

1. **Валидация пользователя:** Отсутствие `X-User-Id` → 400 ошибка
2. **Валидация product_type:** Неподдерживаемый тип → REJECTED
3. **Валидация price:** price ≤ 0 → REJECTED
4. **Валидация payload:** Отсутствуют обязательные поля → REJECTED
5. **Проверка баланса:** Недостаточно средств → PAYMENT_FAILED

### Ключевые шаги

- **Синхронная часть (шаги 1-8):** Создание заказа и возврат ответа клиенту
- **Асинхронная часть (шаги 9-16):** Обработка оплаты через Kafka
- **Финальные статусы:** PAID или PAYMENT_FAILED

---

## 6. Sequence Diagram: Идемпотентность (повторный запрос с тем же order_id)

### Описание
Payment Service получает повторное событие с тем же `order_id`. Система распознаёт дубликат и не списывает средства повторно.

### Диаграмма

```mermaid
sequenceDiagram
    autonumber
    participant K as Kafka
    participant PS as Payment Service
    participant PDB as Payment DB

    Note over K,PS: Первое событие (order_id=123)
    K->>PS: OrderPaymentRequested (event_id=A, order_id=123)
    PS->>PDB: SELECT inbox WHERE event_id=A
    PDB-->>PS: NOT EXISTS
    PS->>PDB: INSERT inbox (event_id=A)
    PS->>PDB: SELECT processed_payments WHERE order_id=123
    PDB-->>PS: NOT EXISTS
    PS->>PDB: UPDATE accounts SET balance=balance-100
    PS->>PDB: INSERT processed_payments (order_id=123)
    PS->>PDB: INSERT outbox (OrderPaymentCompleted)
    PS->>K: ACK

    Note over K,PS: Повторное событие (тот же order_id, другой event_id)
    K->>PS: OrderPaymentRequested (event_id=B, order_id=123)
    PS->>PDB: SELECT inbox WHERE event_id=B
    PDB-->>PS: NOT EXISTS
    PS->>PDB: INSERT inbox (event_id=B)
    PS->>PDB: SELECT processed_payments WHERE order_id=123
    PDB-->>PS: EXISTS (уже обработан)
    PS->>PDB: SELECT accounts WHERE user_id=?
    PS->>PDB: INSERT outbox (OrderPaymentCompleted, current_balance)
    PS->>K: ACK

    Note over PS: Баланс не изменился, средства не списаны повторно
```

### Механизмы идемпотентности

1. **Inbox (event_id):** Защита от повторной обработки того же события
2. **Processed Payments (order_id):** Защита от повторного списания по тому же заказу

### Результат
- Первое событие: списание средств, сохранение в `processed_payments`
- Повторное событие: обнаружение в `processed_payments`, возврат текущего баланса без списания
- Баланс остаётся корректным

---

## 7. Sequence Diagram: Concurrent Operations (конкурентные операции)

### Описание
Два параллельных запроса на списание средств. Optimistic locking (@Version) предотвращает lost updates.

### Диаграмма

```mermaid
sequenceDiagram
    autonumber
    participant T1 as Транзакция 1
    participant T2 as Транзакция 2
    participant DB as Payment DB

    Note over T1,DB: Начальное состояние: balance=1000, version=1

    T1->>DB: BEGIN TRANSACTION
    T2->>DB: BEGIN TRANSACTION

    T1->>DB: SELECT accounts WHERE user_id=?<br/>(balance=1000, version=1)
    T2->>DB: SELECT accounts WHERE user_id=?<br/>(balance=1000, version=1)

    T1->>DB: UPDATE accounts<br/>SET balance=900, version=2<br/>WHERE id=? AND version=1
    DB-->>T1: 1 row updated

    T2->>DB: UPDATE accounts<br/>SET balance=800, version=2<br/>WHERE id=? AND version=1
    DB-->>T2: 0 rows updated (version mismatch)

    T1->>DB: COMMIT
    T2->>DB: ROLLBACK (OptimisticLockException)

    Note over T1,DB: Финальное состояние: balance=900, version=2
    Note over T2: Транзакция 2 отклонена, клиент получит ошибку
```

### Механизм Optimistic Locking

1. **Чтение:** SELECT возвращает текущие `balance` и `version`
2. **Обновление:** UPDATE включает `WHERE version=?`
3. **Проверка:** Если обновлено 0 строк – версия изменилась (конфликт)
4. **Результат:** JPA выбрасывает `OptimisticLockException`, транзакция откатывается

### Результат
- Транзакция 1: успешное списание, version увеличен
- Транзакция 2: отклонена из-за конфликта версий
- Баланс корректен, lost updates предотвращены

---

## Заключение

Диаграммы потоков демонстрируют:
- **Асинхронную коммуникацию** через Kafka с гарантированной доставкой (Outbox/Inbox)
- **Идемпотентность** операций (двойная защита: event_id + order_id)
- **Конкурентность** с защитой от lost updates (optimistic locking)
- **Жизненный цикл** заказов с чёткими переходами между статусами
- **Обработку ошибок** на всех этапах (валидация, оплата, доставка событий)
