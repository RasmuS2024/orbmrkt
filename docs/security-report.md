# Security Scan Report

## Инструменты

| Инструмент | Версия | Режим | Результат |
|-----------|--------|-------|-----------|
| Gitleaks | — | `detect --report-format json` (git) | 0 находок |
| Gitleaks | — | `detect --report-format json` (filesystem) | 0 находок |
| Semgrep OSS | 1.165.0 | SAST (default Java rules) | 0 находок |
| Semgrep OSS (extended) | 1.165.0 | SAST (default Java rules, full workspace) | 0 находок, 1 parsing error |
| Semgrep Pro | 1.165.0 | SAST Pro Engine | **4 находки** |

## Таблица триажа

| ID | Инструмент | Находка | Файл | Критичность | TP/FP | Решение |
|----|-----------|---------|------|-------------|-------|---------|
| 1 | Gitleaks | Секреты в git-истории | — | — | TP (clean) | 0 найденных secret'ов за всю историю репозитория |
| 2 | Gitleaks | Секреты в workspace | — | — | TP (clean) | 0 найденных secret'ов в файловой системе |
| 3 | Semgrep OSS | SAST (default rules) | — | — | TP (clean) | 0 findings |
| 4 | Semgrep OSS | Parsing error в `gradlew` | `gradlew` | Info | FP | Стандартный Gradle wrapper-скрипт, не security-issue |
| 5 | Semgrep Pro | `@RequestMapping` без HTTP-метода | `FallbackController.java:16` | Medium | TP | Заменить на `@GetMapping` |
| 6 | Semgrep Pro | `@RequestMapping` без HTTP-метода | `FallbackController.java:26` | Medium | TP | Заменить на `@GetMapping` |
| 7 | Semgrep Pro | Unsafe reflection: `Class.forName()` | `order-service/OutboxPollingWorker.java:52` | High | TP | Заменить на whitelist/switch по известным типам событий |
| 8 | Semgrep Pro | Unsafe reflection: `Class.forName()` | `payment-service/OutboxPollingWorker.java:52` | High | TP | Заменить на whitelist/switch по известным типам событий |

## Комментарии

**ID 5–6 (TP, rule: `unrestricted-request-mapping`).**
`@RequestMapping` без указания HTTP-метода — потенциальная CSRF-уязвимость
(CWE-352).
- **Достижимость:** FallbackController вызывается API Gateway при срабатывании
  Circuit Breaker. Пользовательский запрос может достичь этих методов через
  любой HTTP-метод (GET, POST, PUT, DELETE).
- **Контекст:** методы только возвращают 503 и не меняют состояние. CSRF-риск
  отсутствует, но правило считает это нарушением.
- **Влияние:** низкое — fallback не выполняет state-changing операций.
Решение: заменить на `@GetMapping`.

**ID 7–8 (TP, rule: `unsafe-reflection`).**
`Class.forName(event.getEventType())` — загрузка класса по строковому имени
из БД.
- **Достижимость:** OutboxPollingWorker (scheduled-задача), `eventType`
  читается из таблицы outbox. Прямого пользовательского ввода нет.
- **Контекст:** `eventType` заполняется через `getClass().getName()` при записи
  в outbox — только известные DTO (`OrderPaymentRequested`,
  `OrderPaymentCompleted`, `OrderPaymentFailed`).
- **Влияние:** низкое — атакующему нужен прямой доступ к БД для подмены.
Решение: заменить reflection на whitelist (switch по константам или Map
известных классов).

## Заключение

Semgrep Pro выявил 4 реальные находки (TP):
- **2×** `@RequestMapping` без HTTP-метода (Medium) — FallbackController
- **2×** `Class.forName()` unsafe reflection (High) — OutboxPollingWorker

Gitleaks и Semgrep OSS — 0 уязвимостей. Проект следует базовым best practices
безопасности: параметризованные запросы, отсутствие hardcoded credentials.
