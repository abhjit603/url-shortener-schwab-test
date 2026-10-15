# Capability Spec: F60001-E50001 -- Short-link lifecycle, analytics, and local durability

| Field | Value |
|---|---|
| Feature ID | F60001 |
| Capability (Epic) ID | E50001 |
| Feature Slug | url-shortener |
| Traces to | ../outcome-spec.md |

**Source:** created **Authoritative:** this file

**Layer discipline** — each layer (L0–L6) has ONE accountable owner and is append-only: update only the layer you own; cross-layer edits go through that layer's owner.

## L0 — Metadata & Status · owner: Capability Owner

- **Lifecycle state:** Draft
- **Traces to Outcome:** O-01, O-02, O-03 · C-01

## L1 — Capability Intent · owner: Capability Owner

- **Outcome:** O-01 create/resolve with a usable short URL; O-02 UTC minute-bucketed click counts (including known-zero); O-03 restart durability and predictable errors
- **Mission:** Ship a URL shortener as one Spring Boot process an operator can run on one machine
- **Problem Statement:** No local URL shortener exists to mint a usable short URL, visit it, inspect counts, deactivate it, and keep data after restart
- **Expected value / scope boundary:** In — create (usable `shortUrl`), public resolve, admin lookup/list/deactivate, UTC minute counts, durable H2 file store, health with a real store query, duplicate-create safety via DB constraint, Problem Details errors, operator docs after implementation. Out — auth/RBAC, external DB and horizontal scale, custom domains, expiry, rate limit, abuse/URL-safety fetch, metrics/tracing/SLOs, backup/HA/multi-region, product web UI, referrer/geo. Admin vs public is `/api/admin` vs `GET /{code}` only; no enforced roles.

## L2 — HL Solution Framing & Constraints · owner: Solution architecture

- **Recommended Approach:** One new Spring Boot app in this repository; one process; embedded H2 file store at a configurable stable path (default `./data/url-shortener`); operator/admin and visitor/user surfaces on the same process; no auth
- **Key Constraints / must-not-break:** One machine; one process; never fall back to in-memory H2; never trust the host header for `shortUrl`; never fetch the destination
- **Dependencies:** Local JVM and embedded H2 file only. Affected service: url-shortener (NEW). No other processes.

## L3 — Solution Blueprint · owner: Solution architecture

- **Reuse / new / gap:** Greenfield Spring Boot conventions in `solution-arch.md`. New: 7-char unique codes; public `302`; admin create/lookup/list/deactivate; UTC minute clicks with increment before redirect; H2 file; `/health` store query; unique `active_destination_key`; RFC 9457. No remaining product gap.

## L4 — Agentic Missions Breakdown · owner: Decomposer

- **Breakdown:** `decomposition.md` — S30001 foundation; S30002 Greenfield core (create+resolve); S30003 Brownfield analytics/reliability/docs. DAG: S30001 → S30002 → S30003. Optional process notes: repo `README.md`.
- **Decomposer addendum (2026-10-15T04:10:43Z):** S30004 adds bounded local redirect metadata caching after the S30003 analytics baseline. Future multi-instance deployment couples server-database migration with shared-cache adoption and cross-instance invalidation design in one work item.

## L5 — Validation Criteria · owner: Validator

| ID | Criterion | Traces to |
|---|---|---|
| AC-01 | `POST /api/admin/links` valid destination → `201/200` with code, usable `shortUrl`, destination, active, createdAt; visitor `GET /{code}` → `302` + `Cache-Control: no-store` | O-01 / C-01 |
| AC-02 | Admin inactive lookup `200 active=false`; deactivate idempotent `200`; further public visits `410`; analytics history retained | C-01 |
| AC-03 | Successful active redirects increment UTC minute buckets (`bucketStart`, `count`) plus code, total, bucketUnit; known-zero `200 total 0 empty buckets`; inactive known readable; unknown `404`; increment commits before `302`; store failure on increment `503` and no redirect | O-02 / C-01 |
| AC-04 | After one process restart, the H2 file at the configured data path still resolves previously created active links | O-03 / C-01 |
| AC-05 | Errors use `application/problem+json` (RFC 9457) with aligned code values (`INVALID_URL`, `INVALID_CURSOR`, `LINK_NOT_FOUND`, `LINK_INACTIVE`, `SHORT_CODE_COLLISION_EXHAUSTED`, `INTERNAL_ERROR`, `STORE_UNAVAILABLE`); malformed list cursors return `400 INVALID_CURSOR`; no silent success; no stack traces, SQL, paths, or destination secrets | O-03 / C-01 |
| AC-06 | At most one active link per normalized destination, enforced by DB unique constraint in a transaction (not check-then-insert); concurrent loser reads the winner and returns `200`; remint after deactivate uses a new code | C-01 |
| AC-07 | Unauthenticated `GET /health` runs a lightweight store query: `200 UP` when reachable; `503 STORE_UNAVAILABLE` otherwise; startup fails fast if the store cannot be opened; no in-memory fallback | C-01 |
| AC-08 | `GET /api/admin/links` returns cursor pages (default 20, max 100), ordered by `createdAt DESC, code DESC`; optional active filter; response contains `links` and nullable `nextCursor` | C-01 |
| AC-09 | After implementation, developer README and user/API guide cover prerequisites, start command, configurable port/base URL/data path, API examples, restart persistence, and reset/cleanup. Build command is documented only after the build tool is selected | G5 / C-01 |

- **Evidence for Done:** Local create → visit → count → deactivate → restart → re-resolve plus known-zero analytics, concurrent duplicate create, health up/down, and the documented error codes; 10/10 happy-path pairs (Assumption O-01)

## L6 — Release & Ops Readiness · owner: Release

- **Readiness:** `environments.md` (forward — not authored)
- **Ops notes:** one process on one machine; configurable port, base URL, and data path; health is `/health`; optional process notes in repo `README.md`

## Open Questions

None for duplicate policy, health, normalization, data path, create response, or analytics zero state. Numeric KPI values remain Assumptions on the Outcome.
