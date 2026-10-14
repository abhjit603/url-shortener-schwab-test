# Environments: url-shortener — URL Shortener Service

Local single-process deployment only (no cluster).

## Cluster Access

N/A — laptop process.

## Environment catalogue

| id | kind | url-ref | test-cmd | gate |
|---|---|---|---|---|
| local | dev-laptop | `http://localhost:${server.port}` | `mvn -q test` | must pass |

## Services Under Test

| service | note |
|---|---|
| url-shortener | Spring Boot on configured `server.port` (default 8080) |
