# 23 — OpenAPI / Swagger Plan

Tooling: **springdoc-openapi** (webmvc-ui). Spec served at `/v3/api-docs`, UI at
`/swagger-ui` (disabled in prod or admin-gated). The catalog in doc 03 is the content
contract; this file defines how it is expressed.

## Security schemes

```yaml
components:
  securitySchemes:
    bearerAuth: {type: http, scheme: bearer, bearerFormat: JWT}
```
Global `security: [bearerAuth]`; explicitly empty security on: `POST /auth/login`,
`POST /auth/refresh`, both WhatsApp webhook operations, actuator health.
Each operation documents its **minimum role** in the description
(source: `docs/API-MASTER-INDEX.md`).

## Tags (endpoint groups → controllers 1:1)

`Auth · Users · Plants · Lines · Settings · Machines · Machine Aliases · Resolution ·
Failure Modes · Parts · Maintenance Records · Imports · Validation · Search · Assistant ·
Analytics · Patterns · WhatsApp · Dashboard · Reports · Jobs · Audit · Notifications`

## Shared components

- `PageResponse<T>` — generic wrapper (`content, page, size, totalElements, totalPages`);
  documented once, parameterized via springdoc generics support.
- `ApiError` — the doc-17 envelope; referenced by every 4xx/5xx response. Reusable response
  refs: `BadRequest, Unauthorized, Forbidden, NotFound, Conflict, UnprocessableEntity,
  TooManyRequests, ServerError, UpstreamUnavailable(424)`.
- Common parameter refs: `page, size, sort, plantId, lineId, machineId, failureModeId,
  dateFrom, dateTo, status, q` (`docs 03 conventions`).
- Enums documented as schemas: `Role, LineKind, RecordKind, RecordSource, ImportStatus,
  RowStatus, ValidationReason, ValidationStatus, PatternType, PatternStatus, JobType,
  JobStatus, BlockType, BlockLabel(FACT|CALCULATED|HYPOTHESIS), ResolutionMethod, WhatsAppState`.

## Request/response DTO documentation rules

- Every DTO field: description + example + constraints (mirrors Bean Validation — springdoc
  picks them up automatically).
- Canonical examples come from the PDF demo data (doc 25): the `MTR brng noise L3 conv…`
  record, the 6205ZZ usage payload, the "37 × Conv Motor-3" alias-group summary, the full
  assistant answer with FACT/CALCULATED/HYPOTHESIS blocks — so Swagger doubles as a product demo.
- Multipart upload (`POST /imports`) documented with `requestBody: multipart/form-data`
  (file + `meta` JSON part). File downloads documented as `application/octet-stream`.
- Async endpoints (`202`) document the `{jobId}` body + a link description to `GET /jobs/{id}`.
- Partial-success bulk responses documented with both branches in one schema
  (`succeeded, failed[]`).

## Assistant block schema (published contract)

`AnswerBlock` = oneOf `SummaryBlock | KeyFailuresBlock | StatisticsBlock | PatternBlock |
HypothesisBlock` discriminated by `type`, each carrying mandatory `label`. StatisticsBlock
requires `tool` + `coverage`; HypothesisBlock requires `confidence` + `disclaimer` — the
trust rules are schema-enforced, and any client rendering the spec inherits them.

## Versioning & lifecycle in the spec

- `info.version` tracks the app; servers list `/{env}` bases; all paths under `/api/v1`.
- Deprecations: `deprecated: true` + `Sunset` header note ≥ 2 minor versions before removal.
- CI check (FUTURE-NICE): generated spec diffed against a committed snapshot to catch
  accidental breaking changes.

## Pagination/sort/filter documentation

Documented once in the spec description (top-level `info.description` includes the doc-03
conventions block) + per-endpoint whitelisted `sort` values enumerated in the parameter
description — no endpoint invents its own paging dialect.
