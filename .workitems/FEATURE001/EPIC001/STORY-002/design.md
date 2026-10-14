---
type: design
id: S30002
epic: E50001
status: approved
title: Greenfield core link lifecycle
---

# Design — S30002 Greenfield core link lifecycle

## Summary

Admin link lifecycle under `/api/admin/links` and public resolve `GET /{code}` with 7-character path constraint. Normalization and duplicate-active semantics per `solution-arch.md`. OpenAPI via springdoc. Analytics on redirect deferred to S30003 (same codebase adds increment later without changing S30002 contracts).

## Architecture

| Component | Role |
|---|---|
| `UrlNormalizer` | HLD normalization rules → `INVALID_URL` + field errors |
| `ShortCodeGenerator` | 7-char alphabet; DB unique on `code` |
| `ShortLink` | `active_destination_key` unique when active |
| `LinkService` | Create (201/200), list, get, deactivate, resolve |
| `LinkCursorCodec` | Opaque continuation token for stable keyset pagination |
| `AdminLinkController` | Admin REST |
| `RedirectController` | 302/404/410; path regex excludes Swagger paths |
| LinkMapper / DTOs | JSON contract |

## Decisions

- Duplicate create: insert + catch `DataIntegrityViolationException` → read active winner → 200.
- Deactivate: set `active=false`, `active_destination_key=null`; remint new code on recreate.
- `shortUrl = app.base-url + / + code`.
- Public inactive: **410**; admin lookup inactive: **200**.
- List order is `createdAt DESC, code DESC`; cursor carries both keys and the page response is `{links, nextCursor}`.

## AC trace

| AC | Evidence |
|---|---|
| AC-S02-01 | `UrlNormalizer`, `UrlNormalizerTest` |
| AC-S02-02 | `ShortCodeGenerator`, unique code, 409 on exhaustion |
| AC-S02-03 | `POST /api/admin/links`, integration test |
| AC-S02-04 | Cursor list/get/deactivate + remint in `UrlShortenerIntegrationTest` |
| AC-S02-05 | Redirect 302/410; 404 for unknown code pattern |
| AC-S02-06 | springdoc; `UrlNormalizerTest`, `LinkCursorCodecTest`, and `UrlShortenerIntegrationTest` |

## Repos

`test-cost` — `src/main/java/com/urlshortener/**`, `src/test/**`.
