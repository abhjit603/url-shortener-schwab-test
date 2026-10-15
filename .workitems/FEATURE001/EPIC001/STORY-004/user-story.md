---
type: user-story
id: S30004
feature: url-shortener
epic: E50001
title: Bounded local redirect metadata cache
owner: abhjit603
status: implemented
created: 2026-10-15T04:10:43Z
updated: 2026-10-15T04:37:46Z
---

# User Story: F60001-E50001-S30004 -- Bounded local redirect metadata cache

## Story

As an operator, I want repeated public redirects to reuse bounded in-process link metadata, so that hot-code lookups put less read load on H2 while redirect, deactivation, and analytics behavior remain correct.

**Status:** implemented
**Scenario:** Enhancement

## Acceptance Criteria

- [x] **AC-S04-01:** Public redirect resolution uses a Caffeine cache-aside lookup keyed by short code. It stores only an immutable snapshot of code, destination, and active state; H2 remains authoritative. Admin lookup/list and click analytics continue to use their existing persistence paths.
- [x] **AC-S04-02:** Cache size and expire-after-write TTL are configurable, have bounded defaults (`10000` entries and `5m`), and are documented. Unknown codes are not cached. Cache eviction or expiry cannot change the public `302`, `404`, or `410` contract.
- [x] **AC-S04-03:** A committed deactivation invalidates the affected cache entry before the deactivate request returns. Cache fill and invalidation are coordinated so a concurrent miss cannot repopulate a stale active value after deactivation commits. A failed/rolled-back deactivation does not leave the cached active state inconsistent with H2.
- [x] **AC-S04-04:** A redirect using cached metadata still increments the current UTC minute bucket and commits the increment before `302`. Failed analytics persistence still returns `503` without redirect. Public redirects retain `Cache-Control: no-store`.
- [x] **AC-S04-05:** Caffeine hit, miss, eviction, and current-size statistics are recorded in the in-process metrics registry without exposing a new Actuator HTTP endpoint or adding short-code labels.
- [x] **AC-S04-06:** Automated coverage verifies warm hits avoid repeated repository loads, deactivation after cache warm-up returns `410` without incrementing clicks, cache-load/deactivation race behavior, rollback behavior, expiry/size bounds, and unchanged redirect analytics contract.
- [x] **AC-S04-07:** Record before/after repository-read counts and a local redirect-path timing comparison. Treat results as local evidence only; do not claim a service-wide latency or throughput guarantee.

## Out of Scope

- Redis or another shared/distributed cache
- Replacing H2, adding multiple application instances, or changing public/admin API contracts
- Negative caching, analytics caching, cache warming, or cache invalidation endpoints
- SLOs, production load guarantees, and public metrics endpoint changes

## Dependencies

- Upstream: S30002 (link lifecycle and redirect contract), S30003 (analytics redirect behavior)
- Downstream: future coupled database/shared-cache scale-out work; it must include both the server database migration and shared-cache consistency design in one work item
- Target repo: `test-cost`. Layers: BE, IT. Wave 3. Classification: enhancement.

## Notes

- Cache is process-local and disposable. Restarting the service repopulates entries from H2.
- Deactivation correctness takes precedence over a cache hit; an in-flight redirect resolved before deactivation may complete, but a resolution after the deactivate response must observe inactive state.
- Cache bounds and the initial TTL are starting configuration values. Tune them only from measured memory and lookup behavior.
