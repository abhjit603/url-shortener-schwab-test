---
type: user-story
id: S30002
feature: url-shortener
epic: E50001
title: Greenfield core link lifecycle
owner: abhjit603
status: draft
created: 2026-10-14T16:29:38Z
updated: 2026-10-14T16:49:30Z
---

# User Story: F60001-E50001-S30002 -- Greenfield core link lifecycle

## Story

As an operator, I want to create, list, look up, and deactivate short links with unique-code minting and at most one active link per normalized destination, and as a visitor I want a basic public resolve, so that I receive a usable `shortUrl`, can retire a link, and Greenfield ends at working create+resolve.

**Status:** implemented (evaluation)
**Scenario:** Greenfield

## Acceptance Criteria

- [ ] **AC-S02-01:** Create runs `UrlNormalizer` HLD rules in order (trim, length ≤ 2048, absolute `http/https`, no user-info, host required, lowercase scheme/host, IDN ASCII, drop fragment, preserve path/query, empty path → `/`); failure is `400 INVALID_URL` with `validationErrors[{field:destination}]`. Destinations are never fetched.
- [ ] **AC-S02-02:** Codes are 7 characters from `23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz`; uniqueness is a DB constraint; retry insert up to 5 times, then `409 SHORT_CODE_COLLISION_EXHAUSTED` (AC-05).
- [ ] **AC-S02-03:** `POST /api/admin/links` with a valid destination returns `201` (new) or `200` (existing active) with code, usable `shortUrl` (`app.base-url + / + code`, never Host), destination, active, `createdAt` (AC-01). At most one active row per normalized destination via unique nullable `active_destination_key` inside a transaction (not check-then-insert); concurrent loser reads the winner and returns `200` (AC-06).
- [ ] **AC-S02-04:** `GET /api/admin/links` uses opaque cursor pagination, default `limit=20`, max 100, ordered by `createdAt DESC, code DESC`; optional active filter. The response contains `links` and nullable `nextCursor`; malformed cursors return `400 INVALID_CURSOR` (AC-08, AC-05). `GET /api/admin/links/{code}` returns 200 including inactive (`active=false`); unknown is 404. `POST .../{code}/deactivate` is idempotent 200 with the current resource and clears `active_destination_key`. Recreate of that destination mints a new code (201); old history stays on the old code (AC-02, AC-06).
- [ ] **AC-S02-05:** Basic public `GET /{code}`: active → `302 Location` = stored destination and `Cache-Control: no-store`; unknown → `404 LINK_NOT_FOUND`; inactive → `410 LINK_INACTIVE` (AC-01, AC-02, AC-05). No click increment and no analytics JSON in this story.
- [ ] **AC-S02-06:** The Greenfield baseline includes an OpenAPI contract for health, admin lifecycle, errors, and public resolve; unit tests cover normalization and cursor validation, and integration tests cover create → resolve, duplicate create, cursor traversal, deactivate, concurrency, and Problem Details before the baseline gate.

## Out of Scope

- Click analytics, increment-before-302, known-zero/inactive history, store-failure-on-increment (S30003)
- Impact/regression/rollback evidence packs and operator docs (S30003)
- Product web UI, authentication, operator-chosen codes, expiry

## Dependencies

- Upstream: S30001
- Downstream: S30003
- Target repo: `test-cost`. Layers: BE. Wave 2. Classification: provider-first.
- Core baseline: this story's create+resolve behavior is the working baseline S30003 must start from.

## Notes

- ACs are release-verifiable HTTP/store checks mapped to AC-01, AC-02, AC-05, AC-06, AC-08.
- Greenfield complete when create and public resolve both work without analytics.
