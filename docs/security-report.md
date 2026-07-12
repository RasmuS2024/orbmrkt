# Security Scan Report

## Инструменты

| Инструмент | Версия | Режим | Результат |
|-----------|--------|-------|-----------|
| Gitleaks | – | `detect --report-format json` (git) | 0 находок |
| Gitleaks | – | `detect --report-format json` (filesystem) | 0 находок |
| Semgrep OSS | 1.165.0 | SAST (default Java rules) | 0 находок |
| Semgrep OSS (extended) | 1.165.0 | SAST (default Java rules, full workspace) | 0 находок, 1 parsing error |
| Semgrep Pro | 1.165.0 | SAST Pro Engine (post-fix) | 0 находок ✅ |

## Таблица триажа

| ID | Инструмент | Находка | Файл | Критичность | TP/FP | Решение |
|----|-----------|---------|------|-------------|-------|---------|
| 1 | Gitleaks | Секреты в git-истории | – | – | TP (clean) | 0 найденных secret'ов за всю историю репозитория |
| 2 | Gitleaks | Секреты в workspace | – | – | TP (clean) | 0 найденных secret'ов в файловой системе |
| 3 | Semgrep OSS | SAST (default rules) | – | – | TP (clean) | 0 findings |
| 4 | Semgrep OSS | Parsing error в `gradlew` | `gradlew` | Info | FP | Стандартный Gradle wrapper-скрипт, не security-issue |
| 5 | Semgrep Pro | `@RequestMapping` без HTTP-метода | `FallbackController.java:16` | Medium | TP | **Исправлено** → `@GetMapping` |
| 6 | Semgrep Pro | `@RequestMapping` без HTTP-метода | `FallbackController.java:26` | Medium | TP | **Исправлено** → `@GetMapping` |
| 7 | Semgrep Pro | Unsafe reflection: `Class.forName()` | `order-service/OutboxPollingWorker.java:52` | High | TP | **Исправлено** → whitelist `Map<String, Class<?>>` |
| 8 | Semgrep Pro | Unsafe reflection: `Class.forName()` | `payment-service/OutboxPollingWorker.java:52` | High | TP | **Исправлено** → whitelist `Map<String, Class<?>>` |

## Комментарии

### ID 5–6 (TP, rule: `unrestricted-request-mapping`) – Исправлено
`@RequestMapping` без указания HTTP-метода – потенциальная CSRF-уязвимость (CWE-352).
- **Достижимость:** FallbackController вызывается API Gateway при срабатывании Circuit Breaker.
- **Контекст:** методы только возвращают 503 и не меняют состояние.
- **Влияние:** низкое.
- **Исправление:** `@RequestMapping` → `@GetMapping` в обоих методах.

### ID 7–8 (TP, rule: `unsafe-reflection`) – Исправлено
`Class.forName(event.getEventType())` – загрузка класса по строковому имени из БД.
- **Достижимость:** OutboxPollingWorker (scheduled-задача), `eventType` читается из таблицы outbox.
- **Контекст:** `eventType` заполняется через `getClass().getName()` при записи в outbox – только известные DTO.
- **Влияние:** низкое.
- **Исправление:** `Class.forName()` заменён на whitelist `Map<String, Class<?>>` с тремя известными типами (`OrderPaymentRequested`, `OrderPaymentCompleted`, `OrderPaymentFailed`). Неизвестный тип → warning + `continue`.

## Заключение

Semgrep Pro (post-fix) – **0 находок**. Все ранее выявленные уязвимости (ID 5–8) исправлены.

Gitleaks и Semgrep OSS – 0 уязвимостей. Проект следует базовым best practices
безопасности: параметризованные запросы, отсутствие жестко заданных (хардкодных) учетных данных.
