---
type: user-story
id: S30003
feature: url-shortener
epic: E50001
title: Brownfield analytics, reliability, and documentation
owner: abhjit603
status: draft
created: 2026-10-14T16:29:38Z
updated: 2026-10-14T16:46:20Z
---

# User Story: F60001-E50001-S30003 -- Brownfield analytics, reliability, and documentation

## Story

As an operator, I want UTC minute-bucketed counts, restart/concurrency/error regression, and README plus user/API guide on top of the existing Greenfield core, so that visits are counted only after a successful increment and Brownfield changes are evidenced against the S30002 baseline.

**Status:** implemented (evaluation)
**Scenario:** Brownfield

## Acceptance Criteria

- [ ] **AC-S03-01: Baseline gate** — Existing S30001/S30002 behavior (process/health/errors, create/cursor-list/lookup/deactivate, public `302/404/410` + `Cache-Control: no-store`) is the unchanged working core; this story starts only after that gate.
- [ ] **AC-S03-02:** Add synchronous UTC minute analytics to the existing redirect: increment transaction commits **before** `302`; if the increment/store write fails, respond `503 STORE_UNAVAILABLE` Problem Details and no redirect (AC-01, AC-03, AC-05).
- [ ] **AC-S03-03:** `GET /api/admin/links/{code}/clicks` returns `{ code, total, bucketUnit: "minute", buckets: [{ bucketStart, count }] }`; known-zero is `200` with total 0 and empty buckets; inactive known history remains readable; unknown is `404` (AC-03).
- [ ] **AC-S03-04: Regression** — After one process restart using the same `app.data-path`, previously created active links still `302` and click history remains readable (AC-04); concurrent duplicate-create loser still returns `200`; documented error codes still match S30001/S30002 (AC-05, AC-06).
- [ ] **AC-S03-05:** Impact analysis of this story vs the S30002 baseline, before/after evidence, and rollback proof that restores the S30002 core baseline without analytics.
- [ ] **AC-S03-06:** Structured logs for create/redirect/deactivate/reject/store-error omit full destination query strings. Repo `README.md` plus user/API guide cover setup, API examples, restart/reset; `ASDLC-README.md` indexes the `.workitems` artifacts (G5 / AC-09).

## Out of Scope

- Changing S30001/S30002 contracts except by adding analytics onto redirect
- Expiry (future scope); product web UI; auth; referrer/geo; prescribing Maven vs Gradle
- Separate orchestration stories (optional process is README checklists only)

## Dependencies

- Upstream: S30001, S30002 (core baseline gate)
- Downstream: none
- Target repo: `test-cost`. Layers: BE, IT. Wave 3. Classification: independent (same service).

## Notes

- Brownfield starts from the Greenfield create+resolve baseline; analytics and evidence are additive.
- ACs map to AC-01, AC-02, AC-03, AC-04, AC-05, AC-09 / G5.
