# SCA Report (Software Composition Analysis)

**Инструмент:** Grype 0.115.0  
**Дата скана:** 2026-07-12

## 1. Статус SCA (до / после исправлений)

| Severity | До | После |
|----------|----|-------|
| Critical | 6  | 0 |
| High     | 26 | 0 |
| Medium   | 31 | 0 |
| Low      | 9  | 0 |

**Закрыто:** 6 Critical, 26 High, 31 Medium, 9 Low  
**Открыто (not-fixed):** 0 – все устранено

## 2. Выполненные исправления

| Пакет | Было | Стало | Механизм |
|-------|------|-------|----------|
| Spring Boot | 3.5.13 | **3.5.16** | обновление BOM |
| Spring Cloud | 2025.0.0 | **2025.0.3** | обновление BOM |
| bcprov-jdk18on | 1.80 | **1.84** | explicit override (api-gateway) |
| commons-lang3 | 3.17.0 | **3.18.0** | explicit override (root `subprojects`) |
| logback-core | 1.5.32 | **1.5.35** | explicit override (root `subprojects`) |
| Tomcat (tomcat-embed-core) | 10.1.53 | **10.1.55** | управляется через SB 3.5.16 |
| Jackson (jackson-databind) | 2.21.2 | **2.21.5** | explicit override (root `subprojects`) |
| Netty (netty-*) | 4.1.132 | **4.1.135** | управляется через SB 3.5.16 |
| PostgreSQL (postgresql) | 42.7.10 | **42.7.11** | управляется через SB 3.5.16 |
| Spring Kafka (spring-kafka) | 3.3.14 | **3.3.16** | управляется через SB 3.5.16 |

## 3. Остаточный риск (not-fixed)

Нет — все уязвимости устранены.

## 4. Вывод

- Устранена **71 уязвимость** (все Critical, High, Medium и Low)
- CVE-2026-54515 закрыт обновлением `jackson-databind` до 2.21.5
