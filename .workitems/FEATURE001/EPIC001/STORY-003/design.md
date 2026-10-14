---
type: design
id: S30003
epic: E50001
status: approved
title: Brownfield analytics, reliability, and documentation
---

# Design — S30003 Brownfield analytics, reliability, and documentation

## Summary

Additive change on S30002 baseline: UTC minute click buckets, increment-before-302, clicks read API, structured logging, operator docs. Core admin/redirect contracts unchanged except active redirects now increment counts.

## Architecture

| Component | Role |
|---|---|
| `ClickBucket` | `(code, bucketStart)` unique; UTC minute truncation |
| `AnalyticsService` | `incrementCurrentMinute` in transaction before 302; stats by code |
| `ClickBucketRepository` | Upsert + increment query |
| `AdminLinkController` | `GET .../{code}/clicks` |
| `RedirectController` | Increment then 302; store failure → 503, no redirect |
| `RequestLoggingFilter` | MDC `traceId`, event/code/outcome; no destination in logs |
| Docs | `README.md`, `docs/API.md` |

## Baseline / rollback

- **Before:** S30002 create/list/deactivate/redirect without counts.
- **After:** Same endpoints + `/clicks`; redirect increments when store is OK.
- **Rollback:** Remove analytics call from redirect and clicks endpoint; entities optional to leave in DB.

## AC trace

| AC | Evidence |
|---|---|
| AC-S03-01 | Integration test lifecycle still passes |
| AC-S03-02 | Increment in TX before `ResponseEntity` 302; catch → 503 |
| AC-S03-03 | `GET /api/admin/links/{code}/clicks` in integration test |
| AC-S03-04 | File H2 under `app.data-path`; restart manual + tests |
| AC-S03-05 | Impact section above |
| AC-S03-06 | Log pattern in `application.yml`; README + `docs/API.md` |

## Repos

`test-cost` only.
