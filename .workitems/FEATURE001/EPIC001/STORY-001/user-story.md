---
type: user-story
id: S30001
feature: url-shortener
epic: E50001
title: Service foundation and error contract
owner: abhjit603
status: draft
created: 2026-10-14T16:29:38Z
updated: 2026-10-14T16:46:20Z
---

# User Story: F60001-E50001-S30001 -- Service foundation and error contract

## Story

As an operator, I want one Spring Boot process with a configurable port, base URL, and H2 file path, a health check that queries the store, and RFC 9457 Problem Details, so that the service starts only with a durable store and fails in a predictable way.

**Status:** implemented (evaluation)
**Scenario:** Greenfield

## Acceptance Criteria

- [ ] **AC-S01-01:** One process binds `server.port` (default `8080`); `app.base-url` (default `http://localhost:${server.port}`) and `app.data-path` (default `./data/url-shortener`) are configurable; the data directory is created if missing (AC-04, AC-07).
- [ ] **AC-S01-02:** If the H2 file store cannot be opened, startup fails and the process does not listen; there is never an in-memory fallback (AC-07).
- [ ] **AC-S01-03:** Unauthenticated `GET /health` runs a lightweight store query: `200 {"status":"UP"}` when reachable; otherwise `503 STORE_UNAVAILABLE` `application/problem+json` (AC-07, AC-05).
- [ ] **AC-S01-04:** A shared RFC 9457 handler emits `application/problem+json` (type, title, status, detail, instance, code, timestamp, `validationErrors` when applicable) for `INVALID_URL`, `LINK_NOT_FOUND`, `LINK_INACTIVE`, `SHORT_CODE_COLLISION_EXHAUSTED`, `INTERNAL_ERROR`, `STORE_UNAVAILABLE`; no stack traces, SQL, filesystem paths, or destination secrets (AC-05).
- [ ] **AC-S01-05:** The durable store is the H2 file under the configured data path (not an in-memory database) so later restart can reopen the same files (AC-04).
- [x] **AC-S01-06:** Spring Boot Actuator exposes health, liveness, and readiness probes. Liveness is process-only; readiness checks the H2 data source. `/livez` and `/readyz` are available on the application port, and the existing `/health` response contract is preserved.

## Out of Scope

- Admin create/list/lookup/deactivate and public `GET /{code}` (S30002)
- Click analytics and full README/API guide (S30003; README also holds optional process notes)
- Product web UI, authentication, external database, cluster
- Prescribing Maven vs Gradle

## Dependencies

- Upstream: none
- Downstream: S30002, S30003
- Target repo: `test-cost` (new URL-shortener app). Layers: BE. Wave 0. Classification: infrastructure.

## Notes

- Greenfield start: process, store, health, and error contract only.
- ACs are release-verifiable HTTP/startup/store checks mapped to AC-04, AC-05, AC-07.
- `shortUrl` must never be derived from the `Host` header (consumed by S30002).
