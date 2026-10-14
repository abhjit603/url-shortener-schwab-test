# Tasks — F60001-E50001-S30002

**Status:** implemented (evaluation).

## DAG

`T01 → T02 → T03 → T04`

| ID | Task | Depends | Done |
|---|---|---|---|
| T01 | `ShortLink` entity + `ShortLinkRepository` + unique constraints | S30001 | [x] |
| T02 | `UrlNormalizer` + short-code generator | T01 | [x] |
| T03 | `LinkService` create/list/get/deactivate + admin API | T02 | [x] |
| T04 | Public `RedirectController` (no analytics) + springdoc + integration test | T03 | [x] |

**Note:** T04 redirect path shipped; S30003 adds increment on the same controller without changing status codes for missing/inactive links.

## Verification

- `UrlNormalizerTest`, `LinkCursorCodecTest`, and `UrlShortenerIntegrationTest#createResolveDuplicateAndDeactivate`
- Integration coverage also includes cursor traversal, malformed cursors, concurrent duplicate creates, and file-store restart persistence.
- OpenAPI: `/swagger-ui.html`, `/api-docs`
