# Tasks — F60001-E50001-S30001

**Status:** implemented (evaluation). Refs commits optional.

## DAG

`T01 → T02 → T03 → T04`

| ID | Task | Depends | Done |
|---|---|---|---|
| T01 | Maven/Spring Boot scaffold, `AGENTS.md`, `application.yml` (port, `app.*`) | — | [x] |
| T02 | H2 file `DataSource` + fail-fast directory setup | T01 | [x] |
| T03 | `/health` store probe + RFC 9457 `GlobalExceptionHandler` + `ErrorCode` enum | T02 | [x] |
| T04 | Actuator liveness/readiness probes; preserve `/health`; add probe tests and operator/API documentation | T03 | [x] |

## Verification

- `mvn -q test` — `/health` up/down is covered in `HealthControllerTest`; Actuator health groups and liveness/readiness isolation are covered in `ActuatorProbeIntegrationTest`; the original health contract remains covered in `UrlShortenerIntegrationTest`
- Manual: `GET /health` after run
