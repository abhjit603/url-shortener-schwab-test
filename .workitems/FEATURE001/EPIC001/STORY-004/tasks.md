# Tasks — F60001-E50001-S30004

**Status:** implemented and verified 2026-10-15T04:37:46Z.

## DAG

`T01 → T02 → T03 → T04`

| ID | Task | Depends | Done |
|---|---|---|---|
| T01 | Add Caffeine and validated `app.cache.redirect.max-size` / `ttl` configuration; set bounded defaults | S30003 | [x] |
| T02 | Add immutable redirect-cache value and wire cache-aside lookup into public resolution; implement commit-aware invalidation and cache-fill race protection | T01 | [x] |
| T03 | Register cache statistics with Micrometer and add cache lifecycle, rollback, concurrency, and redirect-contract coverage | T02 | [x] |
| T04 | Capture before/after repository-read and local timing evidence; update architecture and operator docs | T03 | [x] |

## Verification

- `mvn -q test` — **27 tests, 0 failures, 0 errors, 0 skipped**. Includes `LinkServiceCacheTest`, `RedirectMetadataCacheTest`, the full lifecycle integration suite, and Actuator probe tests.
- Live application under OpenJDK 21.0.12.1 on `localhost:18090` with an isolated temporary H2 path: `/health`, `/livez`, and `/readyz` returned `200 UP`; create returned `201`; redirect returned `302` with the stored `Location` and `Cache-Control: no-store`; click total reached 1; deactivate returned `200`; the next redirect returned `410`; unknown code returned `404`.
- Cache verification under OpenJDK 21.0.12.1: the service test observed two repeated resolutions cause one repository lookup. In a live sample on `localhost:18091`, the first redirect took 17.987 ms; the next 20 averaged 2.015 ms (1.853 ms median, 1.626–4.598 ms range). All 21 returned `302` with the expected `Location` and `Cache-Control: no-store`, and click analytics reached 21. This is a single local sample, not a controlled benchmark or production SLO.
- Temporary application data and logs were removed after the live run.
