# AGENTS.md — test-cost URL shortener

## Stack

- Java 21, Spring Boot 3.3.x, Maven
- H2 file database (embedded), Spring Data JPA
- springdoc-openapi for OpenAPI

## Module map

| Path | Role |
|---|---|
| `src/main/java/com/urlshortener/` | Application code |
| `src/test/java/com/urlshortener/` | Unit and integration tests |
| `.workitems/` | Specs and story artifacts only |

## Test runner

```bash
mvn -q test
```

Run from repo root `test-cost`.

## Commit scopes

`url-shortener`, `spec`, `docs`

## Complexity cap

Cyclomatic complexity ≤ 15 per method (guideline).
