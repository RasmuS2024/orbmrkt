# SCA Report (Software Composition Analysis)

**Инструмент:** Grype 0.115.0  
**Дата:** 2026-07-12

## Сводка

| Severity | Unique | Total (с учётом сервисов) |
|----------|--------|--------------------------|
| Critical | 5 | 8 |
| High | ~15 | ~35 |
| Medium | ~10 | ~20 |
| Low | ~5 | ~10 |

## Таблица приоритизации

| Priority | VULNERABILITY | PACKAGE | INSTALLED → FIXED | CVSS | EPSS | KEV | Действие |
|----------|--------------|---------|-------------------|------|------|-----|----------|
| **P1** | CVE-2026-43512 | `tomcat-embed-core` | 10.1.53 → 10.1.55 | 9.8 | 0.01 | Нет | Обновить Spring Boot → подтянет Tomcat |
| **P1** | CVE-2026-41293 | `tomcat-embed-core` | 10.1.53 → 10.1.55 | 9.8 | 0.01 | Нет | Обновить Spring Boot |
| **P1** | CVE-2025-14813 | `bcprov-jdk18on` | 1.80 → 1.80.2 | 9.3 | 0.001 | Нет | Обновить Bouncy Castle |
| **P1/P2** | CVE-2025-41243 | `spring-cloud-gateway-server-webflux` | 4.3.0 → 4.3.1 | 10.0 | 0.03 | Нет | Обновить SC Gateway. **Риск снижен**: actuator/gateway не включён |
| **P1** | CVE-2026-43515 | `tomcat-embed-core` | 10.1.53 → 10.1.55 | 9.1 | 0.01 | Нет | Обновить Spring Boot |
| **P2** | GHSA-wwpq-f5c3-7hvx | `spring-boot` | 3.5.13 → 3.5.14 | — | — | Нет | Обновить Spring Boot |
| **P2** | GHSA-j3rv-43j4-c7qm и др. | `jackson-databind` | 2.21.2 → 2.21.4 | — | — | Нет | Managed через Spring Boot |
| **P2** | GHSA-98qh-xjc8-98pq | `postgresql` | 42.7.10 → 42.7.11 | — | — | Нет | Обновить драйвер |
| **P2** | Multiple | `netty-*` | 4.1.132 → 4.1.133–135 | — | — | Нет | Managed через Spring Boot |
| **P2** | GHSA-xq69-5h5v-x9x4 | `spring-kafka` | 3.3.14 → 3.3.16 | — | — | Нет | Managed через Spring Boot |
| **P3** | Multiple | `logback-core`, `commons-lang3`, `spring-webmvc` | — | Low/Med | <0.01 | Нет | Зафиксировать, принять риск |

## Стратегия исправления

1. **Обновить Spring Boot** `3.5.13 → 3.5.14` — закроет Critical/High для Tomcat, Netty, Jackson, Spring Framework, Spring Kafka
2. **spring-cloud-gateway** `4.3.0 → 4.3.1`
3. **postgresql** `42.7.10 → 42.7.11`
4. **bcprov-jdk18on** (Bouncy Castle) `1.80 → 1.80.2`
5. **commons-lang3** `3.17.0 → 3.18.0`
6. **logback** `1.5.32 → 1.5.35`

## Комментарий

- **CVE-2025-41243** (CVSS 10) — эксплуатация требует включения `management.endpoints.web.exposure.include=gateway`. В проекте используется только `health`. Риск частично компенсирован конфигурацией.
- **CVE-2026-43512, CVE-2026-41293** — удалённые RCE-подобные уязвимости в Tomcat (order-service, payment-service). Критично к исправлению.
