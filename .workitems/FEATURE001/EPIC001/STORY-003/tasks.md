# Tasks — F60001-E50001-S30003

**Status:** implemented (evaluation).

## DAG

`T01 → T02 → T03`

| ID | Task | Depends | Done |
|---|---|---|---|
| T01 | `ClickBucket` entity + repository + `AnalyticsService` | S30002 gate | [x] |
| T02 | Wire increment-before-302 + `GET .../clicks` | T01 | [x] |
| T03 | Structured logging filter + README/API guide | T02 | [x] |

## Verification

- Integration: `UrlShortenerIntegrationTest#createResolveDuplicateAndDeactivate` verifies redirect-before-click-read and retained inactive history
- Integration: `UrlShortenerIntegrationTest#fileStoreSurvivesApplicationContextRestart` verifies links and counts across a fresh application context
- Integration: `UrlShortenerIntegrationTest#concurrentCreatesReuseTheSingleActiveDestination` verifies the unique-destination race
- Docs: run steps in README match `mvn spring-boot:run` and `/health`
