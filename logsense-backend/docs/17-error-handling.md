# 17 — Error Handling

## 1. Standard error envelope (every non-2xx JSON response)

```json
{
  "timestamp": "2026-09-13T10:41:22.318Z",
  "status": 422,
  "code": "MAPPING_NOT_CONFIRMED",
  "message": "Confirm the column mapping before processing this import.",
  "path": "/api/v1/imports/0192f3…/process",
  "traceId": "b1946ac9-4f6d-7c22-…",
  "details": [
    {"field": "downtimeHours", "code": "OUT_OF_RANGE", "message": "must be between 0 and 720"}
  ]
}
```
- `code` is a **stable, documented, SCREAMING_SNAKE** machine key (frontends switch on it,
  never on `message`). `message` is human-readable English; localization is a frontend concern.
- `details[]` present only for field-level validation errors (400/422).
- `traceId` always present; matches server logs (doc 19). Never include stack traces, SQL,
  internal identifiers, or file paths.
- Implemented by one global `@RestControllerAdvice` mapping the exception hierarchy
  (`common.exception`: `NotFoundException`, `ConflictException`, `ValidationFailedException`,
  `AuthenticationException`, `AccessDeniedException`, `RateLimitedException`,
  `UpstreamUnavailableException`, fallback).

## 2. Status code semantics (uniform)

| Status | Meaning here |
|---|---|
| 400 | malformed request (JSON syntax, wrong types, bad enum, invalid pagination/sort) |
| 401 | missing/invalid/expired credentials; `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `INVALID_REFRESH_TOKEN` |
| 403 | authenticated but role insufficient for an endpoint the user may know exists |
| 404 | resource absent **or outside the caller's plant scope** (anti-probing, doc 16 §3) |
| 409 | state conflict: duplicates, already-resolved, invalid transition, concurrent edit |
| 413 / 415 | upload too large / unsupported file type |
| 422 | well-formed but semantically invalid (field validation, business rule) |
| 423 | account disabled |
| 424 | required upstream (LLM/embedding/WhatsApp provider) unavailable after retries |
| 429 | rate limited (`Retry-After` header) |
| 500 | unexpected; generic message + traceId only |

## 3. Common error code catalog

**Auth/user**: `INVALID_CREDENTIALS · TOKEN_EXPIRED · INVALID_REFRESH_TOKEN · ACCOUNT_DISABLED ·
WEAK_PASSWORD · EMAIL_ALREADY_EXISTS · PHONE_ALREADY_REGISTERED · INVALID_ROLE`
**Master data**: `PLANT_NOT_FOUND · PLANT_CODE_EXISTS · LINE_CODE_EXISTS · LINE_HAS_MACHINES ·
MACHINE_NOT_FOUND · MACHINE_CODE_EXISTS · PART_CODE_EXISTS · FAILURE_MODE_IN_USE · MERGE_TARGET_REQUIRED`
**Aliases**: `ALIAS_ALREADY_MAPPED · ALIAS_TOO_SHORT`
**Records**: `RECORD_NOT_FOUND · REASON_REQUIRED · DATE_IN_FUTURE · DOWNTIME_OUT_OF_RANGE`
**Imports**: `FILE_TOO_LARGE · UNSUPPORTED_FILE_TYPE · DUPLICATE_FILE · NOT_PARSED_YET ·
MAPPING_NOT_CONFIRMED · REQUIRED_TARGET_MISSING · ALREADY_PROCESSING · ALREADY_COMPLETED ·
NOT_CANCELLABLE · NOTHING_TO_RETRY`
**Validation**: `VALIDATION_ITEM_NOT_FOUND · ALREADY_RESOLVED · GROUP_NOT_FOUND · BULK_LIMIT_EXCEEDED`
**Search/assistant**: `QUERY_TOO_SHORT · QUESTION_EMPTY · CONVERSATION_NOT_FOUND · LLM_UNAVAILABLE ·
EMBEDDING_UNAVAILABLE`
**Patterns**: `PATTERN_NOT_FOUND · INVALID_TRANSITION · NOTE_REQUIRED · SCAN_ALREADY_RUNNING`
**Jobs/reports**: `JOB_NOT_FOUND · NOT_READY`
**Generic**: `VALIDATION_FAILED · RESOURCE_NOT_FOUND · CONFLICT · RATE_LIMITED · INTERNAL_ERROR`

## 4. Partial success (bulk & pipeline)

Bulk endpoints (validation bulk-approve/reject, alias-group map) return **200 with a result
report** `{succeeded, failed:[{id, code}]}` — a bulk call is not failed by one row. Import
pipeline uses row-level statuses, never a job-level 500 for data problems (doc 09 §3).

## 5. Degradation rules

- LLM down: assistant returns deterministic blocks + `warnings:[{code:"LLM_UNAVAILABLE"}]`
  when tools alone can answer (STAT_QUERY), else 424; ingestion pauses at NORMALIZE, rows
  retryable — never half-written records.
- Embedding provider down: keyword-only search + warning flag; embedding backfill job catches up.
- WhatsApp send failure: retried with backoff; conversation state persisted, webhook always 200.
