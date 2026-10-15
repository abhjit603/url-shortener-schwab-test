---
type: design
id: S30004
epic: E50001
status: approved
title: Bounded local redirect metadata cache
created: 2026-10-15T04:10:43Z
updated: 2026-10-15T04:21:16Z
---

# Design — S30004 Bounded local redirect metadata cache

## Summary

Add a bounded Caffeine cache-aside lookup to the existing public redirect path. This is an in-process optimization for the current one-process/H2 deployment. The database remains authoritative; APIs, HTTP cache headers, and analytics commit ordering do not change.

## Architecture

| Component | Role |
|---|---|
| `LinkService` | Resolve public redirect metadata through Caffeine; continue to use repositories for admin operations |
| Caffeine cache | Store immutable `(code, destination, active)` snapshots by code; maximum 10,000 entries; expire after write at 5 minutes by default |
| `ShortLinkRepository` | Authoritative lookup on cache miss; continue to own persisted state |
| `AnalyticsService` | Increment UTC minute bucket after active resolution and before the `302`; never cached |
| Micrometer | Record cache hit, miss, eviction, and current-size statistics; no new HTTP endpoint |

## Decisions

- Add Caffeine as the local cache implementation; do not add Redis or a generic cache abstraction for one cache use.
- Use a bounded maximum size and expire-after-write TTL. Configure both under `app.cache.redirect.*`; the defaults are starting values, not capacity or freshness guarantees.
- Cache immutable values, not JPA entities. Do not cache unknown codes or click counts.
- Invalidate the affected code after a create/deactivate transaction commits. Coordinate cache misses with deactivation so an older in-flight database read cannot refill an active value after the committed invalidation. Rollback does not publish invalidation as if the write committed.
- Leave HTTP `Cache-Control: no-store` unchanged; internal lookup caching does not authorize browser or intermediary caching.
- Future multi-instance work must migrate the H2 file store and adopt a shared cache together, with cross-instance fill/invalidation ordering and deactivation consistency defined as part of one work item.

## Consistency Boundary

When a deactivate request returns successfully, any subsequent redirect resolution observes the committed inactive state. A request that already resolved the active link before deactivation may finish. Do not weaken this contract to accommodate cache behavior.

## AC trace

| AC | Evidence |
|---|---|
| AC-S04-01 | `LinkServiceCacheTest` verifies repeated resolution uses one repository read and snapshots are immutable |
| AC-S04-02 | `RedirectMetadataCacheTest` covers size/expiry and uncached unknown codes; startup binds the documented defaults |
| AC-S04-03 | `RedirectMetadataCacheTest#invalidationPreventsAnOlderLoadFromRepopulatingStaleState`; `UrlShortenerIntegrationTest#rolledBackDeactivateLeavesWarmCacheAndDatabaseActive` |
| AC-S04-04 | `UrlShortenerIntegrationTest#createResolveDuplicateDeactivateAndAnalyticsLifecycle` and `RedirectControllerTest` |
| AC-S04-05 | `RedirectMetadataCacheTest#cacheStatisticsAreRegisteredWithoutLinkCodeTags`; Actuator integration confirms metrics endpoint is not exposed |
| AC-S04-06 | `mvn -q test` passes all 27 tests |
| AC-S04-07 | Two service resolutions cause one repository read; live sample: cold first redirect 3.06 ms, warm 20-request mean 1.89 ms / median 1.88 ms; informational only |

## Repos

`test-cost` — `src/main/java/com/urlshortener/**`, `src/test/**`, `pom.xml`, and `.workitems/`.
