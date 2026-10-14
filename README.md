# URL Shortener

A single-process Spring Boot service for creating, resolving, managing, and
measuring short links. It uses an embedded H2 file database, so links and click
history survive an application restart on the same machine.

## Features

- Create normalized HTTP/HTTPS short links; repeat creates reuse the active link.
- Resolve active links with a `302` redirect. Deactivated links return `410`.
- List, look up, and deactivate links through the admin API.
- Read click totals in UTC minute buckets, including history for inactive links.
- Expose health, liveness/readiness probes, and OpenAPI endpoints. The admin namespace is not authenticated.

## Technology

- Java 21 and Spring Boot 3.3
- Spring Boot Actuator health probes
- Maven 3.9+
- Spring Data JPA with an H2 file database
- springdoc OpenAPI at `/api-docs` and `/swagger-ui.html`

## Build and Test

Run from the repository root:

```bash
mvn -q test
```

The tests include unit coverage for URL normalization and cursor tokens, plus
HTTP/service integration coverage for link lifecycle, cursor pagination,
concurrent duplicate creates, analytics, file-store restart persistence,
health up/down, and Actuator readiness/liveness behavior.

If Maven Central is needed instead of the configured repository, use:

```bash
mvn -q test -s .mvn/settings-local-central.xml
```

## Run Locally

```bash
mvn spring-boot:run
```

The service listens on `http://localhost:8080` by default. If startup reports a
missing `app.data-path`, set the documented default explicitly:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.data-path=./data/url-shortener"
```

Use the Maven Central settings file if dependency resolution requires it:

```bash
mvn spring-boot:run -s .mvn/settings-local-central.xml
```

## Configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8080` | HTTP listen port |
| `app.base-url` | `http://localhost:${server.port}` | Base used for returned `shortUrl`; never derived from `Host` |
| `app.data-path` | `./data/url-shortener` | Directory for the H2 file database |

Set values in `src/main/resources/application.yml`, environment variables, or
Spring command-line arguments. For example:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090 --app.base-url=http://localhost:9090 --app.data-path=./data/url-shortener"
```

The app creates the data directory when needed and does not fall back to an
in-memory database. Stop the process before removing the directory to reset
local data.

## API Quick Start

Check the store:

```bash
curl -i http://localhost:8080/health
```

For deployment probes, use `/livez` for process liveness and `/readyz` for
readiness (which includes the H2 data source). Actuator equivalents are
`/actuator/health/liveness` and `/actuator/health/readiness`. The legacy
`/health` route remains available with its original response contract.

Create a link:

```bash
curl -i -X POST http://localhost:8080/api/admin/links \
  -H 'Content-Type: application/json' \
  -d '{"destination":"https://example.com/"}'
```

The response includes a `code` and `shortUrl`. Open the returned URL to redirect
to the destination. The list endpoint uses cursor pagination:

```text
GET /api/admin/links?limit=20&active=true
GET /api/admin/links?limit=20&active=true&cursor=<nextCursor>
```

List responses contain `links` and `nextCursor`; pass the returned cursor to
continue, and keep the same `active` filter across pages. Full request/response
examples and status codes are in [docs/API.md](docs/API.md). OpenAPI UI:
http://localhost:8080/swagger-ui.html.

## Project Structure

- `src/main/java/com/urlshortener/` — application, services, persistence, and HTTP API
- `src/test/java/com/urlshortener/` — unit and integration tests
- `docs/API.md` — API contract and examples
- `.workitems/` — outcome, capability, architecture, story, and validation artifacts
- `ASDLC-README.md` — guide to the `.workitems` artifacts

## Workitem Process Notes

These process notes are not product requirements. For an ambiguous requirement,
record the decision and its trace to the affected workitem before changing
behavior. Validate the S30002 lifecycle baseline before S30003
analytics/reliability work, then run the full test suite and the local health
and create/redirect/click smoke path.
