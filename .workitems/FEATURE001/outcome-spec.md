---
type: outcome-spec
id: F60001
slug: url-shortener
title: URL Shortener Service
owner: abhjit603
status: draft
created: 2026-10-14T14:44:59Z
updated: 2026-10-14T17:00:53Z
---

# Outcome Spec: F60001 -- URL Shortener Service

## Status

| Status | Gate Owner | Decision Date | Decision Notes |
|---|---|---|---|
| Draft | TBD | TBD | Generated as Draft; not self-approved |

## Intent Classification

**Primary intent:** Both — an operator/admin must do create, lookup, deactivate, list, and read counts; a visitor/user must do resolve; success is also a result: a usable short URL, durable local single-system storage, and predictable errors within bounded side-project scope.

## Background

Independent URL shortener service. One Java Spring Boot process on one machine with embedded durable storage.

## Problem

There is no local URL shortener an operator can use to mint a usable short URL, a visitor can open to reach the destination, and an operator can use to inspect counts, deactivate links, and keep data across a process restart.

## Assumptions

1. O-01/O-02/O-03 numbers are **Assumptions** — no KPI was supplied.
2. One capability. Deactivate is in scope; calendar expiry is out. Duplicate-create policy is closed in the HLD: reuse the existing active link for a normalized destination; remint a new code after deactivate.
3. Referrer/geo are non-goals. One Spring Boot process; no cluster. No authentication in this scope; admin and public surfaces are separated by URL namespace only.

## Outcomes

| ID | Outcome | Quantitative Target | Constraint | Validation Method |
|---|---|---|---|---|
| O-01 | Operator creates a usable short URL; visitor reaches the destination | **Assumption:** 10/10 valid create-then-visit pairs succeed on one local process | One machine; no custom domains; no auth | Execute 10 valid create-then-visit pairs; each create returns a usable `shortUrl`; each visit receives `302` to the stored destination |
| O-02 | Operator reads click counts over time | **Assumption:** each successful active redirect increments that link; UTC minute buckets are readable, including known-zero (`total 0`, empty buckets) | No referrer/geo; no hourly/two-hour window required | Inspect counts before and after visits on UTC minute buckets |
| O-03 | Service stays correct across restart and bad input | **Assumption:** previously created active links still resolve after one restart | Predictable Problem Details errors; no multi-node | Restart then re-resolve; invalid URL, unknown code, inactive public code, and store-unavailable paths |

## Capabilities

| ID | Capability | Description | Priority |
|---|---|---|---|
| C-01 | URL shortener service | Create/resolve; admin lookup/list/deactivate; UTC minute-bucketed counts; durable H2 file store; health; duplicate-create safety | Must |

## Engineering Scenarios

Epic delivery is **app-only** (three stories: S30001 → S30002 → S30003). Optional process notes for ambiguous requirements and release/orchestration live in repo `README.md`, not separate stories.

- **Greenfield:** S30001 foundation and S30002 create/resolve core (OpenAPI + tests).
- **Brownfield:** S30003 analytics, reliability, regression, and documentation on the S30002 baseline.

## Requirements

### General

| # | Requirement | Details | Priority |
|---|---|---|---|
| G1 | Create and resolve | Operator supplies a long URL and receives a usable short URL; visitor reaches the destination | High |
| G2 | Operator management | Lookup (including inactive); list; deactivate so visitors no longer reach it | High |
| G3 | Click analytics | Successful active redirects counted in UTC minute buckets, readable by the operator (including known-zero and inactive history) | High |
| G4 | Local durability and correctness | Durable across restart on a configurable H2 file path; predictable errors; health that queries the store; safe concurrent duplicate creates | High |
| G5 | Operator documentation | After implementation, a developer README and a user/API guide explain prerequisites, start command, configurable port/base URL/data path, API examples, restart persistence, and reset/cleanup | High |

**Entity Model** — Business object: short link (normalized destination, unique code, active/inactive, click counts over UTC minutes).

**UI** — No product web UI in this scope. Personas use HTTP API surfaces. A built-in local target page, if added at all, is an optional developer convenience and is not a correctness dependency.

## Goals

- Operator can mint a usable short URL, look up, list, deactivate, and inspect UTC minute-bucketed click counts on one machine; visitor reaches the destination; data survives restart; bad input and unknown or inactive public codes fail predictably.

## Non-Goals

- Authentication/RBAC; external database or horizontal scale; distributed ID/code allocation or cache.
- Async analytics/events or richer dimensions; custom domains, expiry, rate limiting, abuse controls, or URL-safety blocklist.
- Product metrics/tracing/SLOs; backup/restore, HA, multi-region, billing, or multi-node cluster.
- Product web UI or mobile apps.
- Built-in local target page as a required feature (optional developer convenience only).

## Use Cases

| # | UC | Actor | Flow | Result |
|---|---|---|---|---|
| UC-01 | Create short link | Operator | Submit a valid long URL | Usable short URL; `201` new or `200` existing active for the same normalized destination |
| UC-02 | Visit short link | Visitor | Open an active short link | `302` to destination; count increases after the increment commits |
| UC-03 | Lookup and deactivate | Operator | Look up a link and deactivate it | Admin still reads it (`active=false`); further public visits return `410`; history retained |
| UC-04 | Inspect analytics | Operator | Read counts over time | UTC minute buckets; known-zero is `200` with `total 0`; inactive remains visible |
| UC-05 | Survive restart | Operator | Restart the process, then resolve an existing active link | Active links still resolve from the H2 file |
| UC-06 | Bad input / unknown or inactive code | Either | Invalid URL, unknown code, or public inactive code | Problem Details error; no silent success; no count on unknown/inactive |
| UC-07 | List short links | Operator | Traverse a bounded list using an opaque cursor | Newest first by `createdAt` then code; default 20, max 100; response returns `links` and `nextCursor` |

## Open Questions

- Numeric KPIs not given — O-01/O-02/O-03 use labeled Assumptions.
- Duplicate-create, health, normalization, data path, create response, and analytics zero state are closed in the HLD.
