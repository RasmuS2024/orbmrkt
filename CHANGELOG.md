# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.1.1] - 2026-07-30

### Added

- Kafka DLQ topic (`{topic}.dlq`) for outbox events after max attempts exhausted
- Releases section and CHANGELOG

### Fixed

- Dead letter error logging on outbox max attempts

## [v1.1.0] - 2026-07-30

### Added

- OpenAPI/Swagger documentation for all REST endpoints and DTOs (`@Tag`, `@Operation`, `@Schema`, `@ApiResponse`)
- `OpenApiConfig.java` with `@SecurityScheme` in order-service and payment-service
- `docker-compose.prod.yml` with tagged images from GHCR
- Production deployment section in README

### Changed

- Replaced em dash "—" with en dash "–" in Java files and README

## [v1.0.3] - 2026-07-24

### Changed

- CI: parallel security scan, path filters, develop trigger
- Added `--parallel` flag to Gradle build

## [v1.0.2] - 2026-07-24

### Added

- CI: tag-triggered docker push
- `.dockerignore`
- Increased `start_period` to 90s for service health checks

## [v1.0.1] - 2026-07-23

### Changed

- Replaced `BigDecimal` price with `long` (avoid floating-point precision issues)
- Updated documentation and `analytics.sql`

## [v1.0.0] - 2026-07-21

### Added

- Initial release: order-service, payment-service, api-gateway
- Event-driven architecture with Kafka (Outbox + Inbox patterns)
- Basic REST API for orders and payments
- CI pipeline with test, security scan, and docker build
