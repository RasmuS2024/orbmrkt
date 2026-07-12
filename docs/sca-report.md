# SCA Report (Software Composition Analysis)

**Инструмент:** Grype 0.115.0  
**Дата скана:** 2026-07-12

## 1. Статус SCA (до / после исправлений)

| Severity | До | После |
|----------|----|-------|
| Critical | 6  | 0 |
| High     | 26 | 0 |
| Medium   | 31 | 1 |
| Low      | 9  | 0 |

**Закрыто:** 6 Critical, 26 High, 30 Medium, 9 Low  
**Открыто (not-fixed):** 1 Medium — risk accepted

## 2. Выполненные исправления

| Пакет | Было | Стало | Механизм |
|-------|------|-------|----------|
| Spring Boot | 3.5.13 | **3.5.16** | обновление BOM |
| Spring Cloud | 2025.0.0 | **2025.0.3** | обновление BOM |
| bcprov-jdk18on | 1.80 | **1.84** | explicit override (api-gateway) |
| commons-lang3 | 3.17.0 | **3.18.0** | explicit override (root `subprojects`) |
| logback-core | 1.5.32 | **1.5.35** | explicit override (root `subprojects`) |
| Tomcat (tomcat-embed-core) | 10.1.53 | **10.1.55** | управляется через SB 3.5.16 |
| Jackson (jackson-databind) | 2.21.2 | **2.21.4** | управляется через SB 3.5.16 |
| Netty (netty-*) | 4.1.132 | **4.1.135** | управляется через SB 3.5.16 |
| PostgreSQL (postgresql) | 42.7.10 | **42.7.11** | управляется через SB 3.5.16 |
| Spring Kafka (spring-kafka) | 3.3.14 | **3.3.16** | управляется через SB 3.5.16 |

## 3. Остаточный риск (not-fixed)

| Vulnerability | Пакет | Severity | CVSS | Обоснование |
|---------------|-------|----------|------|-------------|
| GHSA-5jmj-h7xm-6q6v (CVE-2026-54515) | `jackson-databind` 2.21.4 | Medium | 5.3 | Нет исправления. Обход десериализации без учёта регистра через `@JsonTypeInfo(use=Id.NAME)`. В проекте Jackson используется штатно через Spring Boot/Kafka без кастомной конфигурации `@JsonTypeInfo`. Эксплуатация требует специфичной настройки десериализации. **Риск принят.** |

## 4. Вывод

- Устранена **71 уязвимость** (все Critical, High, Low и 30 из 31 Medium)
- Осталась **1 Medium** с состоянием `not-fixed` — эксплуатация маловероятна в контексте проекта, риск принят
- Следующий SCA-скан запланирован после выхода фикса для GHSA-5jmj-h7xm-6q6v (CVE-2026-54515) в Jackson BOM
