---
id: E50001
status: draft
updated: 2026-10-15T04:21:16Z
---

# Decomposition: E50001 -- Short-link lifecycle, analytics, and local durability

## Scenario Mapping (app stories)

| Scenario | Stories | Notes |
|---|---|---|
| Greenfield | S30001 → S30002 | Foundation then create/resolve core (OpenAPI + tests) |
| Brownfield | S30003 | Analytics/reliability/docs on S30002 baseline |
| Enhancement | S30004 | Bounded in-process redirect metadata cache on S30003 baseline |

Ambiguous-requirement and orchestration/release process notes live in repo `README.md` (not separate stories).

## Affected Services

| Service | Change Type | Scope |
|---|---|---|
| url-shortener (NEW) | New | One Spring Boot app in `test-cost` |

## Dependency Subgraph

```mermaid
graph LR
  S30001[S30001 foundation] --> S30002[S30002 core lifecycle]
  S30002 -->|core baseline| S30003[S30003 analytics and docs]
  S30003 -->|redirect contract and analytics baseline| S30004[S30004 local redirect cache]
```

## Wave Ordering

| Wave | Stories | Classification |
|---|---|---|
| 0 — Infrastructure | S30001 | infrastructure |
| 1 — Providers | S30002 | provider-first |
| 2 — Enhancement | S30003 | independent (Brownfield) |
| 3 — Enhancement | S30004 | additive cache |

## Mission Specifications

### Mission: S30001 -- Service foundation and error contract

- **Target repos:** test-cost · **Layers:** BE · **Wave:** 0 · **Scenario:** Greenfield
- **Dependencies:** none · **Size:** S · **Tokens:** ~150k
- **AC:** configurable port/base-url/data-path; fail-fast no in-memory; `/health` store query; RFC 9457; H2 file store.

### Mission: S30002 -- Greenfield core link lifecycle

- **Target repos:** test-cost · **Layers:** BE · **Wave:** 1 · **Scenario:** Greenfield
- **Dependencies:** S30001 · **Size:** M · **Tokens:** ~400k
- **AC:** UrlNormalizer; 7-char codes; create/list/lookup/deactivate; duplicate constraint; public `302/404/410`; OpenAPI + unit/integration baseline.

### Mission: S30003 -- Brownfield analytics, reliability, and documentation

- **Target repos:** test-cost · **Layers:** BE, IT · **Wave:** 2 · **Scenario:** Brownfield
- **Dependencies:** S30001, S30002 (baseline) · **Size:** M · **Tokens:** ~400k
- **AC:** minute analytics on redirect; known-zero/inactive history; restart/concurrency/error regression; impact/rollback notes; logs; README + API guide (including optional process notes).

### Mission: S30004 -- Bounded local redirect metadata cache

- **Target repos:** test-cost · **Layers:** BE, IT · **Wave:** 3 · **Scenario:** Enhancement
- **Dependencies:** S30002, S30003 (completed redirect and analytics baseline) · **Size:** M · **Tokens:** ~400k
- **AC:** bounded Caffeine cache for immutable redirect metadata; configurable maximum size and TTL; commit-aware deactivation invalidation with cache-load race protection; preserve HTTP and analytics contracts; tests, cache statistics, and before/after repository-read evidence.
- **Implementation:** completed and verified 2026-10-15T04:21:16Z; see `STORY-004/tasks.md` for test and live-run evidence.

## Estimation (cost_est)

| Mission | Size | Effort | Tokens |
|---|---:|---:|---:|
| S30001 | S | 2h | ~150k |
| S30002 | M | 4h | ~400k |
| S30003 | M | 4h | ~400k |
| S30004 | M | 4h | ~400k |
| **Total** | — | **14h** | **~1,350k** |

## Completeness Checks

- [x] Every affected service has at least one Mission
- [x] Greenfield, Brownfield, and cache enhancement covered by S30001–S30004
- [x] Core baseline gates S30002 → S30003 → S30004
- N/A consumer-adapter / event-consumer
- [x] DAG valid; every Mission has AC and cost_est row
