# URL shortener API guide

**Base URL (local default):** `http://localhost:8080` — configure `app.base-url` so `shortUrl` in responses matches how clients reach the service.

**OpenAPI UI:** `/swagger-ui.html` · **JSON:** `/api-docs`

## Health and deployment probes

`GET /health`

- `200` — `{ "status": "UP" }` when the H2 store answers a lightweight query.
- `503` — `STORE_UNAVAILABLE` otherwise.

This legacy route keeps its documented response contract. For orchestrators,
use the Spring Boot Actuator probes instead:

- `GET /actuator/health/liveness` or `GET /livez` — process liveness; does not
  depend on the database.
- `GET /actuator/health/readiness` or `GET /readyz` — ready to serve traffic;
  includes the H2 data source check.
- `GET /actuator/health` — overall Actuator health, including available health
  contributors such as the data source.

Only the Actuator health endpoint is exposed over HTTP. Health details are not
included in public responses.

## Create link

`POST /api/admin/links`

Request:

```json
{ "destination": "https://Example.com/path?q=1" }
```

Responses:

- `201` — new active link (`code`, `shortUrl`, normalized `destination`, `active`, `createdAt`)
- `200` — existing active link for the same normalized destination
- `400 INVALID_URL` — Problem Details with `validationErrors`

`shortUrl` is always `app.base-url + "/" + code` (never derived from `Host`).

## List / lookup / deactivate

- `GET /api/admin/links?limit=20&cursor=<opaque-token>&active=true` — newest first; `limit` maximum is 100. Omit `cursor` for the first page and pass the previous response's `nextCursor` to continue.
- `GET /api/admin/links/{code}` — `200` for active or inactive links; `404` for unknown codes.
- `POST /api/admin/links/{code}/deactivate` — idempotent `200`; clears active-destination uniqueness so a new create mints a new code.

The list response wraps the results and continuation token:

```json
{
  "links": [
    { "code": "Ab3CdEf", "shortUrl": "http://localhost:8080/Ab3CdEf", "destination": "https://example.com/", "active": true, "createdAt": "2026-10-14T17:05:00Z" }
  ],
  "nextCursor": "<opaque-token>"
}
```

`nextCursor` is `null` when there are no more results. Keep the same `active` filter while traversing pages. A malformed cursor returns `400 INVALID_CURSOR`.

## Public redirect

`GET /{code}` (7-character code)

- `302` — `Location: <destination>`, `Cache-Control: no-store` (increment committed first)
- `404 LINK_NOT_FOUND`
- `410 LINK_INACTIVE`
- `503 STORE_UNAVAILABLE` — no redirect if the analytics/store write fails

## Analytics

`GET /api/admin/links/{code}/clicks`

Example response:

```json
{
  "code": "Ab3CdEf",
  "total": 2,
  "bucketUnit": "minute",
  "buckets": [
    { "bucketStart": "2026-10-14T17:05:00Z", "count": 2 }
  ]
}
```

A known link with zero clicks returns `200`, `total: 0`, and `buckets: []`. Inactive links retain click history. An unknown code returns `404`.

## Errors

Errors use RFC 9457 Problem Details (`application/problem+json`) with the extension `code`, which may be one of:

- `INVALID_URL`
- `INVALID_CURSOR`
- `LINK_NOT_FOUND`
- `LINK_INACTIVE`
- `SHORT_CODE_COLLISION_EXHAUSTED`
- `INTERNAL_ERROR`
- `STORE_UNAVAILABLE`
