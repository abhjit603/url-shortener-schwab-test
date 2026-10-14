---
type: design
id: S30001
epic: E50001
status: approved
---

# Design — S30001 Service foundation and error contract

## Summary

Single Spring Boot 3.3 / Java 21 module at `test-cost` root. Embedded H2 file database under configurable `app.data-path`. No admin or public link APIs in this story’s scope; shared infrastructure supports S30002+.

## Architecture

| Layer | Artifacts |
|---|---|
| Config | `application.yml`, `AppProperties`, `DatabaseConfig`, `AppConfig` |
| Persistence | JPA `ddl-auto: update`; tables created when entities land in S30002 |
| Health | `HealthController`, `StoreProbe` (`SELECT COUNT(*) FROM short_link`), Spring Boot Actuator health contributors |
| Errors | `ErrorCode`, `ApiException`, `GlobalExceptionHandler` (RFC 9457 + code, timestamp, validationErrors) |

## Decisions

- Datasource URL: `jdbc:h2:file:${app.data-path}/urlshortener` — directory created at startup; no in-memory fallback.
- `app.base-url` defaults to `http://localhost:${server.port}` for later `shortUrl` construction (never Host header).
- Problem Details enabled via `spring.mvc.problemdetails.enabled`.
- The existing `/health` endpoint remains contract-compatible. Actuator exposes only health over HTTP; liveness is process-only and readiness includes `readinessState,db`. `/livez` and `/readyz` are mapped on the application port.

## AC trace

| AC | Evidence |
|---|---|
| AC-S01-01 | `application.yml` port/base-url/data-path; `DatabaseConfig` mkdir |
| AC-S01-02 | File JDBC only; startup fails if path unusable |
| AC-S01-03 | `GET /health` → `200 UP` or `503 STORE_UNAVAILABLE` |
| AC-S01-04 | Handler codes per HLD; no stack/SQL in responses |
| AC-S01-05 | H2 file path under `./data/url-shortener` (default) |
| AC-S01-06 | Actuator liveness/readiness groups plus `/livez` and `/readyz`; readiness checks the data source; `/health` remains compatible |

## Repos

`test-cost` only (`.workitems/` for specs).
