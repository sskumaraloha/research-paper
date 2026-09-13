# 03 — Complete API Endpoint Catalog

Every REST endpoint in LogSense V1. Companion quick-reference: `docs/API-MASTER-INDEX.md`.

## Conventions (apply to every endpoint)

- Base path `/api/v1`. JSON everywhere except file upload (multipart) and downloads.
- **Auth levels** (JWT bearer unless Public). Roles are per-plant; a level implies all above it:
  `VIEWER < TECHNICIAN < ENGINEER < PLANT_ADMIN < SUPER_ADMIN`. "ENGINEER" below means
  "ENGINEER or higher **on the plant that owns the resource**". SUPER_ADMIN passes everything.
- **Plant scoping**: every non-admin read/write is filtered to plants where the caller holds a
  role. Accessing a resource in a foreign plant → `404` (not `403`, to avoid ID probing).
- **Pagination** on every list: `?page=0&size=20&sort=field,desc` → response wrapper
  `{"content":[...],"page":0,"size":20,"totalElements":n,"totalPages":n}`. Max `size=200`.
- **Common filters** where meaningful: `plantId, lineId, machineId, failureModeId, dateFrom,
  dateTo, status, q`. Dates are `yyyy-MM-dd` (record dates) or ISO-8601 instants (system times).
- **Errors**: standard envelope from `docs/17-error-handling.md`; codes below list only the
  endpoint-specific ones on top of the universal `400/401/403/500`.
- **IDs**: UUIDv7 strings. Soft-deleted resources behave as `404`.
- **Idempotency**: all state-changing pipeline endpoints accept `Idempotency-Key` header.

---

# 1. Auth (`/auth`)

### 1.1 `POST /api/v1/auth/login`
- **Purpose**: authenticate with email + password; issue access + refresh tokens.
- **Auth**: Public. Rate-limited (5/min/IP).
- **Request**: `{"email":"rajesh@plant.in","password":"…"}`
- **Response 200**:
```json
{"accessToken":"…","refreshToken":"…","expiresIn":900,
 "user":{"id":"…","name":"Rajesh Kumar","email":"…",
   "plantRoles":[{"plantId":"…","plantName":"Demo Pune Plant","role":"PLANT_ADMIN"}]}}
```
- **Status**: 200, 400 (malformed), 401 `INVALID_CREDENTIALS`, 423 `ACCOUNT_DISABLED`, 429.
- **Validation**: email format; password non-blank. Never reveal which field was wrong.
- **Entities**: `User`, `RefreshToken`. **Service**: `AuthService`.
- **Rules**: BCrypt verify; rotate refresh token per login; audit `USER_LOGIN`.
- **Workflow**: doc 04 · Workflow 1.

### 1.2 `POST /api/v1/auth/refresh`
- **Purpose**: exchange a valid refresh token for a new access + refresh pair (rotation).
- **Auth**: Public (token is the credential).
- **Request**: `{"refreshToken":"…"}` → **Response 200**: same shape as login (no user block).
- **Status**: 200, 401 `INVALID_REFRESH_TOKEN` (also fired on reuse of a rotated token → all
  sessions for that user revoked).
- **Entities**: `RefreshToken`. **Service**: `AuthService`.

### 1.3 `POST /api/v1/auth/logout`
- **Purpose**: revoke the presented refresh token (access token expires naturally).
- **Auth**: Authenticated. **Request**: `{"refreshToken":"…"}` → **204**.
- **Status**: 204, 401. **Service**: `AuthService`. Audited.

### 1.4 `GET /api/v1/auth/me`
- **Purpose**: current user profile + per-plant roles + effective permissions (drives UI menus).
- **Auth**: Authenticated. **Response 200**: user block as in 1.1 plus `permissions:["VALIDATION_REVIEW",…]`.
- **Status**: 200, 401. **Entities**: `User`, `UserPlantRole`. **Service**: `AuthService`.

### 1.5 `POST /api/v1/auth/change-password`
- **Purpose**: self-service password change; revokes other sessions.
- **Auth**: Authenticated. **Request**: `{"currentPassword":"…","newPassword":"…"}` → **204**.
- **Status**: 204, 400 `WEAK_PASSWORD` (policy: ≥10 chars, not in breach list), 401 wrong current.
- **Service**: `AuthService`. Audited.

---

# 2. Users & Roles (`/users`, `/roles`)

### 2.1 `GET /api/v1/users`
- **Purpose**: list users of plants the caller administers. Filters: `plantId, role, active, q`.
- **Auth**: PLANT_ADMIN. **Response 200**: page of `{id,name,email,phone,active,plantRoles[],lastLoginAt}`.
- **Entities**: `User`, `UserPlantRole`. **Service**: `UserService`.

### 2.2 `POST /api/v1/users`
- **Purpose**: create user with initial per-plant role; sends invite (V1: returns temp password flag).
- **Auth**: PLANT_ADMIN (only for own plants; SUPER_ADMIN anywhere).
- **Request**: `{"name":"…","email":"…","phone":"+91…","plantRoles":[{"plantId":"…","role":"TECHNICIAN"}]}`
- **Response 201**: created user. **Status**: 201, 409 `EMAIL_ALREADY_EXISTS`, 422 (unknown plant/role).
- **Rules**: PLANT_ADMIN cannot grant SUPER_ADMIN; phone required if role TECHNICIAN (WhatsApp identity). Audited.

### 2.3 `GET /api/v1/users/{id}` — PLANT_ADMIN. 200/404. Detail incl. WhatsApp registration status.

### 2.4 `PATCH /api/v1/users/{id}`
- **Purpose**: update name/phone/active. **Auth**: PLANT_ADMIN. **Status**: 200, 404, 409 (phone in use).
- **Rules**: deactivation revokes refresh tokens. Audited with before/after.

### 2.5 `DELETE /api/v1/users/{id}`
- **Purpose**: deactivate (soft; records they authored are preserved). **Auth**: SUPER_ADMIN. **204**/404.

### 2.6 `PUT /api/v1/users/{id}/plant-roles`
- **Purpose**: replace the user's role assignments for plants the caller administers.
- **Auth**: PLANT_ADMIN. **Request**: `{"plantRoles":[{"plantId":"…","role":"ENGINEER"}]}` → 200.
- **Status**: 200, 404, 422 `INVALID_ROLE`. **Entities**: `UserPlantRole`. Audited.

### 2.7 `GET /api/v1/roles`
- **Purpose**: static role catalog with permission descriptions (for admin UI). **Auth**: Authenticated. **200**.

---

# 3. Plants & Lines (`/plants`, `/lines`)

### 3.1 `GET /api/v1/plants` — Authenticated (VIEWER). Own plants only. 200: `{id,code,name,city,timezone,lineCount,machineCount}`.
### 3.2 `POST /api/v1/plants` — SUPER_ADMIN. Request `{"code":"PUN1","name":"Demo Pune Manufacturing Plant","city":"Pune","timezone":"Asia/Kolkata"}` → 201. 409 `PLANT_CODE_EXISTS`. Audited.
### 3.3 `GET /api/v1/plants/{id}` — VIEWER. 200/404. Includes settings summary.
### 3.4 `PATCH /api/v1/plants/{id}` — PLANT_ADMIN. Update name/city/timezone. 200/404/409. Audited.
### 3.5 `GET /api/v1/plants/{id}/lines` — VIEWER. 200: `[{id,code,name,kind:"PRODUCTION|UTILITY",machineCount,active}]`.
### 3.6 `POST /api/v1/plants/{id}/lines` — PLANT_ADMIN. `{"code":"L3","name":"Line 3","kind":"PRODUCTION"}` → 201. 409 `LINE_CODE_EXISTS` (per plant).
### 3.7 `PATCH /api/v1/lines/{id}` — PLANT_ADMIN. 200/404/409.
### 3.8 `DELETE /api/v1/lines/{id}` — PLANT_ADMIN. Soft delete; **409 `LINE_HAS_MACHINES`** if active machines exist. 204/404.
- **Entities**: `Plant`, `Line`. **Services**: `PlantService`, `LineService`.

### 3.9 `GET /api/v1/plants/{id}/settings` / `PUT /api/v1/plants/{id}/settings`
- **Purpose**: plant configuration — date format, shifts (A/B/C), operating hours, downtime
  cost per line-hour, auto-approval confidence threshold (default 0.90), alias-suggestion threshold.
- **Auth**: GET → ENGINEER; PUT → PLANT_ADMIN. **Status**: 200, 404, 422 (threshold ∉ [0.5,1]).
- **Entities**: `PlantSettings`. **Service**: `PlantSettingsService`. Audited.
- **Rule**: thresholds are read by normalization at pipeline time — changing them affects only future rows.

---

# 4. Machines (`/machines`)

### 4.1 `GET /api/v1/machines`
- **Purpose**: machine master list. Filters: `plantId, lineId, status(HEALTHY|ATTENTION|RECURRING), q`
  (q resolves against name/code/type/aliases with shorthand expansion); sorts: `recordCount, downtime, lastEventAt, name`.
- **Auth**: VIEWER. **Response 200** page:
```json
{"id":"…","code":"CONV-L3-MTR-01","name":"Line 3 Conveyor Motor","lineId":"…","lineName":"Line 3",
 "type":"Conveyor Motor","status":"ATTENTION","recordCount":23,"downtimeHours":18.9,"lastEventAt":"2026-08-14"}
```
- **Entities**: `Machine` (+aggregates). **Service**: `MachineService`.

### 4.2 `POST /api/v1/machines`
- **Auth**: ENGINEER. **Request**: `{"lineId":"…","code":"CONV-L3-MTR-01","name":"Line 3 Conveyor Motor","type":"Conveyor Motor","aliases":["L3 conv"]}`
- **Status**: 201, 409 `MACHINE_CODE_EXISTS` (per plant), 422 unknown line. Creates initial aliases. Audited.

### 4.3 `GET /api/v1/machines/{id}` — VIEWER. 200/404. Detail: metadata + aliases + status + last breakdown.
### 4.4 `PATCH /api/v1/machines/{id}` — ENGINEER. Name/type/line move/criticality. 200/404/409. Audited.
### 4.5 `DELETE /api/v1/machines/{id}` — PLANT_ADMIN. Soft; history retained, hidden from lists. 204/404.

### 4.6 `GET /api/v1/machines/{id}/history`
- **Purpose**: paginated maintenance timeline (newest first). Filters: `kind(BREAKDOWN|PM|INSPECTION),
  failureModeId, dateFrom, dateTo, source(IMPORT|WHATSAPP|MANUAL)`.
- **Auth**: VIEWER. **Response 200**: page of maintenance record summaries (see 7.1 shape).
- **Service**: `MaintenanceRecordService`.

### 4.7 `GET /api/v1/machines/{id}/stats`
- **Purpose**: deterministic machine statistics: MTTR, MTBF (observed window), breakdown count,
  total downtime, last failure, repeat-failure count, failure-mode Pareto. Query: `dateFrom,dateTo`.
- **Auth**: VIEWER. **Response 200**:
```json
{"machineId":"…","window":{"from":"2024-09-01","to":"2026-09-13"},
 "records":23,"breakdowns":12,"downtimeHours":18.9,"mttrHours":1.6,"mtbfHours":962,
 "lastFailureAt":"2026-08-14","repeatFailureEvents":8,
 "pareto":[{"failureMode":"Bearing","count":4},{"failureMode":"Overheating","count":2}],
 "computedBy":"DETERMINISTIC"}
```
- **Service**: `MachineStatsService`. **Rule**: every figure SQL-computed; `computedBy` flag is
  contractual (frontend renders the CALCULATED badge from it).

### 4.8 `GET /api/v1/machines/{id}/parts` — VIEWER. Parts consumed on this machine with counts & last-used. **Service**: `PartUsageService`.
### 4.9 `GET /api/v1/machines/{id}/patterns` — VIEWER. Patterns whose evidence involves this machine (see 12.x shape).

---

# 5. Machine Aliases & Resolution (`/machine-aliases`, `/machine-resolution`)

### 5.1 `GET /api/v1/machine-aliases`
- **Purpose**: list aliases; filters `machineId, q, source(SEED|VALIDATION|MANUAL)`.
- **Auth**: ENGINEER. **200** page: `{id,machineId,machineName,alias,normalizedAlias,source,createdBy,createdAt}`.

### 5.2 `POST /api/v1/machine-aliases`
- **Purpose**: manually map an alias to a machine (resolver learns immediately).
- **Auth**: ENGINEER. **Request**: `{"machineId":"…","alias":"Conv Motor-3"}` → **201**.
- **Status**: 201, 404 machine, 409 `ALIAS_ALREADY_MAPPED` (to a *different* machine — returns
  conflicting machine in error details), 422 blank/too short (<3 normalized chars).
- **Entities**: `MachineAlias`. **Service**: `AliasService`. Emits `MachineAliasCreated`. Audited.

### 5.3 `DELETE /api/v1/machine-aliases/{id}` — ENGINEER. 204/404. Audited (records keep their machine link; only future resolution changes).

### 5.4 `POST /api/v1/machine-resolution/resolve`
- **Purpose**: resolve free text to machine candidates (used by UI autocompletes, debugging, and
  internally by pipeline/assistant through the same service).
- **Auth**: Authenticated. **Request**: `{"plantId":"…","text":"CONV MTR 3"}`
- **Response 200**:
```json
{"input":"CONV MTR 3","normalized":"conveyor motor 3",
 "candidates":[{"machineId":"…","name":"Line 3 Conveyor Motor","confidence":0.96,
                "method":"ALIAS|EXACT|FUZZY|SEMANTIC"}],
 "resolved": true}
```
- **Service**: `MachineResolverService`. **Rule**: pure read; never auto-creates aliases.

---

# 6. Failure Modes & Parts (`/failure-modes`, `/parts`)

### 6.1 `GET /api/v1/failure-modes` — VIEWER. Plant taxonomy (Bearing, VFD, Sensor, Belt, Coupling, Alignment, Overheating, Electrical trip, Leakage, Lubrication, Preventive, Inspection, Other) + usage counts. 200.
### 6.2 `POST /api/v1/failure-modes` — PLANT_ADMIN. `{"name":"Cavitation","synonyms":["cavitate"]}` → 201; 409 duplicate name.
### 6.3 `PATCH /api/v1/failure-modes/{id}` — PLANT_ADMIN. Rename / edit synonyms. 200/404/409.
### 6.4 `DELETE /api/v1/failure-modes/{id}` — PLANT_ADMIN. **Request** requires `{"mergeIntoId":"…"}` when records reference it; re-points records, audited. 204/404/422 `MERGE_TARGET_REQUIRED`.
- **Entities**: `FailureMode`. **Service**: `MaintenanceRecordService` (taxonomy section).

### 6.5 `GET /api/v1/parts` — VIEWER. Filters `q, plantId`. 200 page: `{id,code:"6205ZZ",name:"Deep-Groove Ball Bearing 6205ZZ",machineCount,usageCount,lastUsedAt,trend}`. Aggregates SQL-computed.
### 6.6 `POST /api/v1/parts` — ENGINEER. `{"code":"6205ZZ","name":"…"}` → 201; 409 `PART_CODE_EXISTS`.
### 6.7 `GET /api/v1/parts/{id}` — VIEWER. 200/404.
### 6.8 `PATCH /api/v1/parts/{id}` — ENGINEER. 200/404/409.
### 6.9 `GET /api/v1/parts/{id}/usage`
- **Purpose**: answer "6205 bearing kahan kahan lagi hai?" deterministically. Query: `dateFrom,dateTo,plantId`.
- **Auth**: VIEWER. **Response 200**:
```json
{"part":{"id":"…","code":"6205ZZ"},"totalReplacements":10,
 "machines":[{"machineId":"…","name":"Line 3 Conveyor Motor","line":"Line 3","count":4,"sharePct":40.0}],
 "byLine":[{"line":"Line 3","count":6,"sharePct":60.0}],
 "avgIntervalDays":87,"firstUsedAt":"2025-05-20","lastUsedAt":"2026-08-14",
 "timeline":[{"recordId":"…","machineId":"…","date":"2026-08-14"}],
 "computedBy":"DETERMINISTIC"}
```
- **Entities**: `Part`, `RecordPart`, `MaintenanceRecord`. **Service**: `PartUsageService`.

---

# 7. Maintenance Records (`/maintenance-records`)

### 7.1 `GET /api/v1/maintenance-records`
- **Purpose**: global history table. Filters: `plantId, lineId, machineId, failureModeId, kind,
  source, technician, partId, dateFrom, dateTo, q` (q runs the same expansion as search).
- **Auth**: VIEWER. **Response 200** page:
```json
{"id":"…","machineId":"…","machineName":"Line 3 Conveyor Motor","date":"2026-08-14",
 "kind":"BREAKDOWN","failureMode":"Bearing","actionSummary":"Bearing replaced",
 "rawText":"MTR brng noise L3 conv, replcd 6205ZZ, algnmnt chk, OK",
 "downtimeHours":2.0,"technician":"Sunil","parts":["6205ZZ"],
 "source":{"type":"IMPORT","file":"maintenance_log_2026.xlsx","sheet":"Aug-2026","row":143}}
```
- **Service**: `MaintenanceRecordService`.

### 7.2 `POST /api/v1/maintenance-records`
- **Purpose**: manual entry (UI form). Free-text goes through the same normalization service;
  structured fields provided by the user win over extraction.
- **Auth**: TECHNICIAN. **Request**:
```json
{"machineId":"…","date":"2026-09-13","kind":"BREAKDOWN","rawText":"belt slip, tension diya",
 "failureModeId":"…","actionSummary":"Belt re-tensioned","downtimeHours":1.0,
 "partIds":["…"],"technicianName":"Prakash"}
```
- **Status**: 201, 404 machine, 422 (`date` in future; downtime < 0 or > 168; kind invalid).
- **Rules**: creates a `raw_record` (source MANUAL) for provenance symmetry; emits
  `MaintenanceRecordCreated` (→ embedding, stats, pattern check). Audited.

### 7.3 `GET /api/v1/maintenance-records/{id}` — VIEWER. 200/404. Full detail incl. parts, provenance summary, versions count.
### 7.4 `PATCH /api/v1/maintenance-records/{id}`
- **Purpose**: audited correction (wrong machine, downtime, mode). **Auth**: ENGINEER.
- **Status**: 200/404/422. **Rules**: previous values snapshotted to `audit_logs`
  (before/after JSON); raw text never editable; emits `MaintenanceRecordUpdated`.
### 7.5 `DELETE /api/v1/maintenance-records/{id}` — PLANT_ADMIN. Soft delete with mandatory `{"reason":"…"}` body; excluded from search/stats. 204/404/422 `REASON_REQUIRED`. Audited.

### 7.6 `GET /api/v1/maintenance-records/{id}/source`
- **Purpose**: traceability ("View Source" / citation drawer): the immutable raw record + its origin.
- **Auth**: VIEWER. **Response 200**:
```json
{"recordId":"…","raw":{"id":"…","text":"MTR brng noise L3 conv, replcd 6205ZZ, algnmnt chk, OK",
  "source":{"type":"IMPORT","importJobId":"…","file":"maintenance_log_2026.xlsx","sheet":"Aug-2026","row":143}},
 "extraction":{"machineText":"L3 conv","confidence":0.92,"resolvedBy":"ALIAS"},
 "validation":{"status":"AUTO_APPROVED","reviewedBy":null}}
```
- **Entities**: `RawRecord`, `StagedRecord`, `ValidationItem`. **Service**: `RecordProvenanceService`.

---

# 8. Imports / Ingestion (`/imports`)

### 8.1 `POST /api/v1/imports`
- **Purpose**: upload a historical file; creates the import job. Multipart: `file` + JSON part
  `meta = {"plantId":"…","description":"2023 log"}`.
- **Auth**: ENGINEER. **Response 201**: `{"importJobId":"…","status":"CREATED","file":{"name":"maintenance_log_2023.xlsx","sizeBytes":2400000,"contentType":"…"}}`
- **Status**: 201, 413 `FILE_TOO_LARGE` (>50 MB), 415 `UNSUPPORTED_FILE_TYPE` (allow xlsx/xls/csv/pdf/png/jpg), 422 unknown plant.
- **Rules**: file stored immutably (object storage); SHA-256 recorded; duplicate file (same hash+plant) → 409 `DUPLICATE_FILE` with existing job id. Audited.

### 8.2 `GET /api/v1/imports` — ENGINEER. Filters `plantId,status,dateFrom,dateTo`. 200 page of job summaries.
### 8.3 `GET /api/v1/imports/{id}`
- **Purpose**: job status + live progress + result counts.
- **Auth**: ENGINEER. **Response 200**:
```json
{"id":"…","status":"PROCESSING","stage":"RESOLVE_MACHINES","progressPct":64,
 "counts":{"detected":1842,"parsed":1842,"skipped":52,"normalized":1180,
           "autoApproved":0,"needsReview":0,"failed":3},
 "startedAt":"…","finishedAt":null,"file":{…},"mappingConfirmed":true}
```
- Job `status`: `CREATED → PARSED → MAPPING_REQUIRED? → PROCESSING → COMPLETED | FAILED | CANCELLED`.

### 8.4 `GET /api/v1/imports/{id}/preview`
- **Purpose**: first N parsed rows + auto-suggested column mapping for user confirmation.
- **Auth**: ENGINEER. **Response 200**: `{"columns":["Date","M/c","Problem","Action","Hrs"],"suggestedMapping":{"Date":"RECORD_DATE","M/c":"MACHINE_TEXT","Problem":"DESCRIPTION","Action":"ACTION","Hrs":"DOWNTIME"},"sampleRows":[…],"sheets":["Jan-2023",…]}`
- 409 `NOT_PARSED_YET` while parsing.

### 8.5 `PUT /api/v1/imports/{id}/mapping`
- **Purpose**: confirm/override column mapping (per sheet where needed).
- **Auth**: ENGINEER. **Request**: `{"mapping":{"Date":"RECORD_DATE",…},"dateFormat":"dd/MM/yyyy"}` → 200.
- **Status**: 200, 409 `ALREADY_PROCESSING`, 422 `REQUIRED_TARGET_MISSING` (must map at least MACHINE_TEXT + DESCRIPTION or a combined text column).

### 8.6 `POST /api/v1/imports/{id}/process`
- **Purpose**: start the async pipeline (normalize → resolve → route). Returns a `jobId` to poll.
- **Auth**: ENGINEER. **Response 202**: `{"jobId":"…","importJobId":"…","status":"QUEUED"}`
- **Status**: 202, 409 `ALREADY_PROCESSING`/`ALREADY_COMPLETED`, 422 `MAPPING_NOT_CONFIRMED`.
- Idempotent via `Idempotency-Key`.

### 8.7 `POST /api/v1/imports/{id}/cancel` — ENGINEER. Cancels a QUEUED/PROCESSING run at the next row boundary; processed rows keep their state. 200 / 409 `NOT_CANCELLABLE`.
### 8.8 `GET /api/v1/imports/{id}/rows`
- **Purpose**: inspect row outcomes. Filter `status=PARSED|SKIPPED|FAILED|NORMALIZED|NEEDS_REVIEW|APPROVED`, plus `q`.
- **Auth**: ENGINEER. **200** page: `{rawRecordId, rowRef:"Sheet Mar-2023!R143", text, status, reason}`.
### 8.9 `POST /api/v1/imports/{id}/retry` — ENGINEER. Re-runs FAILED rows only (e.g., after LLM outage). 202 with jobId; 409 if nothing to retry.
### 8.10 `GET /api/v1/imports/{id}/file` — ENGINEER. Streams the original file (Content-Disposition). 200/404. Access audited.
- **Entities**: `ImportJob`, `ImportFile`, `RawRecord`, `StagedRecord`, `Job`.
- **Services**: `ImportService`, `ImportPipelineOrchestrator`. Emits `ImportCompleted`.

---

# 9. Validation (`/validation`)

### 9.1 `GET /api/v1/validation/queue`
- **Purpose**: open validation items. Filters: `plantId, importJobId, machineId, reason
  (LOW_MACHINE_CONFIDENCE|LOW_FIELD_CONFIDENCE|MISSING_FIELDS|DUPLICATE_SUSPECT), confMin, confMax`.
- **Auth**: ENGINEER. **Response 200** page:
```json
{"id":"…","raw":{"text":"MTR brng noise L3 conv,…","source":"maintenance_log_2023.xlsx → March-2023 → Row 143"},
 "extracted":{"machine":{"text":"L3 conv","machineId":"…","name":"Line 3 Conveyor Motor","confidence":0.92},
   "failureMode":"Bearing","part":"6205ZZ","action":"Bearing replaced","downtimeHours":2.5,"date":"2023-03-14"},
 "reason":"LOW_MACHINE_CONFIDENCE","overallConfidence":0.92,"status":"OPEN"}
```

### 9.2 `GET /api/v1/validation/summary`
- **Purpose**: queue header numbers + **alias groups** (the "37 records use 'Conv Motor-3'" card).
- **Auth**: ENGINEER. **Response 200**:
```json
{"open":43,"aliasGroups":[{"groupKey":"conv motor-3","aliasText":"Conv Motor-3","count":37,
   "suggested":{"machineId":"…","name":"Line 3 Conveyor Motor","confidence":0.96},
   "samples":["Conv Motor-3 brg noise chk","Conv Motor-3 trip VFD reset"]}],
 "byReason":{"LOW_MACHINE_CONFIDENCE":39,"MISSING_FIELDS":3,"DUPLICATE_SUSPECT":1}}
```
- **Service**: `AliasGroupService` (groups OPEN items by normalized unresolved machine text).

### 9.3 `GET /api/v1/validation/items/{id}` — ENGINEER. Full item: raw, extraction w/ per-field confidence, machine candidates, duplicate suspects. 200/404.

### 9.4 `POST /api/v1/validation/items/{id}/approve`
- **Purpose**: accept extraction as-is → maintenance record created.
- **Auth**: ENGINEER. **Request**: `{}` (optional `note`). **Response 200**: `{"maintenanceRecordId":"…"}`
- **Status**: 200, 404, 409 `ALREADY_RESOLVED` (returns resolving action).
- **Rules**: writes `ValidationAction(APPROVE)`; emits `ValidationCompleted`, `MaintenanceRecordCreated`.

### 9.5 `PUT /api/v1/validation/items/{id}`
- **Purpose**: edit extracted fields then approve in one step.
- **Auth**: ENGINEER. **Request**: corrected fields (same shape as `extracted` above, any subset)
  + `{"approve":true}`. **Response 200** with record id.
- **Status**: 200, 404, 409, 422 field validation. **Rules**: corrections stored on the action
  (before/after) — feeds resolver learning when machine changed.

### 9.6 `POST /api/v1/validation/items/{id}/reject`
- **Auth**: ENGINEER. **Request**: `{"reason":"illegible row"}` (required). **200**; 422 `REASON_REQUIRED`; 409.
- **Rules**: raw record retained with status REJECTED; excluded from index/stats.

### 9.7 `POST /api/v1/validation/bulk-approve` / 9.8 `POST /api/v1/validation/bulk-reject`
- **Auth**: ENGINEER. **Request**: `{"itemIds":["…"],"reason":"…"(reject only)}` (max 500).
- **Response 200**: `{"succeeded":37,"failed":[{"itemId":"…","code":"ALREADY_RESOLVED"}]}` — partial success by design.

### 9.9 `POST /api/v1/validation/alias-groups/map`
- **Purpose**: THE demo moment — map an entire alias group to one machine in a single action.
- **Auth**: ENGINEER. **Request**: `{"groupKey":"conv motor-3","machineId":"…","createAlias":true}`
- **Response 200**: `{"mappedItems":37,"createdRecords":37,"aliasId":"…"}`
- **Status**: 200, 404 machine/group, 409 `ALIAS_ALREADY_MAPPED` elsewhere.
- **Rules**: transactional per item with partial-success report; creates `MachineAlias`
  (source VALIDATION) so the resolver learns; every item gets a `ValidationAction(BULK_MAP)`. Audited.

### 9.10 `GET /api/v1/validation/items/{id}/history` — ENGINEER. All actions on the item (who/when/what changed). 200/404.
- **Entities**: `ValidationItem`, `ValidationAction`, `StagedRecord`. **Service**: `ValidationService`.

---

# 10. Search (`/search`)

### 10.1 `GET /api/v1/search`
- **Purpose**: global omni-search (top-bar). `q` + optional `plantId`; returns grouped hits.
- **Auth**: VIEWER. Example `q=bearing gaya` → **Response 200**:
```json
{"query":"bearing gaya","expanded":["bearing","brng","brg","बेयरिंग"],
 "machines":[{"id":"…","name":"Line 3 Conveyor Motor","matchedOn":"alias"}],
 "parts":[{"id":"…","code":"6205ZZ"}],
 "records":[{"id":"…","machineName":"…","date":"2025-11-11","raw":"bearing gaya L3 conveyor motor…",
   "score":0.94,"why":["SAME_FAILURE_MODE","SEMANTIC_SIMILARITY"]}],
 "patterns":[{"id":"…","title":"Bearing recurrence ~92 days"}]}
```
- **Status**: 200, 422 `QUERY_TOO_SHORT` (<2 chars). **Service**: `SearchService`.

### 10.2 `GET /api/v1/search/records`
- **Purpose**: record-focused hybrid search with pagination + full filters (`machineId, lineId,
  failureModeId, dateFrom, dateTo, source`) and match explanations; used by history search and assistant retrieval.
- **Auth**: VIEWER. **200** page of `{record…, score, why[]}`.
- **Rules**: keyword (FTS+trigram) and semantic (pgvector) branches merged via reciprocal-rank
  fusion; `why[]` ∈ `KEYWORD_MATCH, SHORTHAND_VARIANT, ALIAS_MATCH, SAME_FAILURE_MODE, SHARED_PART, SEMANTIC_SIMILARITY`.
- Architecture: `docs/08-search-architecture.md`.

---

# 11. AI Assistant (`/assistant`)

### 11.1 `POST /api/v1/assistant/query`
- **Purpose**: ask a maintenance question (English/Hindi/Hinglish/shorthand).
- **Auth**: VIEWER. **Request**:
```json
{"conversationId":null,"plantId":"…","question":"Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?",
 "context":{"machineId":null}}
```
- **Response 200** (message with structured, labelled blocks):
```json
{"conversationId":"…","messageId":"…",
 "blocks":[
  {"type":"SUMMARY","label":"FACT","text":"Line 3 Conveyor Motor has 23 maintenance records…"},
  {"type":"KEY_FAILURES","label":"FACT","items":[{"failureMode":"Bearing","count":4}]},
  {"type":"STATISTICS","label":"CALCULATED","stats":{"downtimeHours":18.9,"breakdowns":12,"mttrHours":1.6},
   "coverage":{"recordsIncluded":12,"recordsWithDowntime":12},"tool":"machine_stats"},
  {"type":"PATTERN","label":"CALCULATED","text":"Bearing recurrence ≈ every 92 days (4 events)"},
  {"type":"HYPOTHESIS","label":"HYPOTHESIS","confidence":"POSSIBLE",
   "text":"Repeated bearing replacement despite alignment checks may indicate a mounting/alignment issue.",
   "disclaimer":"Requires engineering validation."}],
 "citations":[{"citationId":"…","recordId":"…","rawRecordId":"…","excerpt":"MTR brng noise L3 conv…"}],
 "resolution":{"machineId":"…","confidence":0.97},
 "meta":{"latencyMs":1840,"toolsUsed":["resolve_machine","machine_stats","search_records"],"model":"claude-sonnet-5"}}
```
- **Status**: 200, 404 (conversation not owned), 422 `QUESTION_EMPTY`, 424 `LLM_UNAVAILABLE`
  (degraded: deterministic blocks still returned when tools ran), 429 rate limit.
- **Entities**: `AiConversation`, `AiMessage`, `AiCitation`. **Service**: `AssistantService`.
- **Rules** (contractual): numeric blocks come only from tool outputs; hypothesis block always
  labelled and never merged into FACT text; every FACT block must carry ≥1 citation or be
  downgraded. Architecture: `docs/07-ai-architecture.md`.

### 11.2 `GET /api/v1/assistant/conversations` — VIEWER (own only). 200 page: `{id,title,lastMessageAt,messageCount}`.
### 11.3 `GET /api/v1/assistant/conversations/{id}` — VIEWER (owner). Full message history with blocks + citations. 200/404.
### 11.4 `DELETE /api/v1/assistant/conversations/{id}` — VIEWER (owner). Soft delete. 204/404.
### 11.5 `GET /api/v1/assistant/messages/{id}/citations`
- **Purpose**: citation list for one answer, each resolvable to the source drawer (7.6 shape). 200/404.
### 11.6 `POST /api/v1/assistant/messages/{id}/feedback`
- **Purpose**: 👍/👎 + optional comment (quality telemetry). **Request**: `{"rating":"UP|DOWN","comment":"…"}` → 204. 404/409 (already rated).

---

# 12. Analytics (`/analytics`) — all responses carry `"computedBy":"DETERMINISTIC"`

Common query params: `plantId (required unless single-plant token), lineId?, machineId?, dateFrom, dateTo, compareToPrevious=true|false`.

### 12.1 `GET /api/v1/analytics/kpis`
- **Purpose**: MTTR, MTBF, total downtime, breakdown count (+% delta vs previous equal-length period), records indexed, validation pending.
- **Auth**: VIEWER. **200**: `{"mttrHours":2.8,"mttrDeltaPct":-18.4,"mtbfHours":184,…,"window":{…}}`
- **Service**: `KpiCalculator`. 422 invalid range (from>to; >5y span).
### 12.2 `GET /api/v1/analytics/downtime-trend` — VIEWER. `granularity=MONTH|WEEK`. 200: `{"points":[{"period":"2026-01","downtimeHours":24.5,"breakdowns":9}]}`.
### 12.3 `GET /api/v1/analytics/pareto` — VIEWER. Failure-mode Pareto with counts + downtime + cumulative %. 200.
### 12.4 `GET /api/v1/analytics/top-machines` — VIEWER. `metric=DOWNTIME|BREAKDOWNS|MTTR&limit=5`. 200 ranked list.
### 12.5 `GET /api/v1/analytics/parts-consumption` — VIEWER. Top parts by replacement count in window. 200.
### 12.6 `GET /api/v1/analytics/lines` — VIEWER. Per-line rollup (downtime, breakdowns, share %) — powers "Line 3 hotspot". 200.
- **Service**: `AnalyticsService`. No LLM anywhere in this module.

---

# 13. Patterns (`/patterns`)

### 13.1 `GET /api/v1/patterns`
- **Purpose**: detected patterns. Filters: `plantId, machineId, type(RECURRENCE|TEMP_FIX_REPEAT|CROSS_MACHINE|PART_CONCENTRATION|DOWNTIME_HOTSPOT), status(DETECTED|UNDER_REVIEW|CONFIRMED|DISMISSED), minEvidence`.
- **Auth**: VIEWER. **200** page:
```json
{"id":"…","type":"RECURRENCE","status":"DETECTED",
 "fact":{"label":"CALCULATED","summary":"4 bearing replacements on Line 3 Conveyor Motor, avg interval 92 days",
   "metrics":{"events":4,"avgIntervalDays":92,"spanMonths":9},"evidenceCount":4},
 "hypothesis":{"label":"HYPOTHESIS","confidence":"POSSIBLE",
   "text":"Possible mounting/alignment issue","disclaimer":"Requires engineering validation"},
 "machineId":"…","detectedAt":"…","lastEvaluatedAt":"…"}
```
### 13.2 `GET /api/v1/patterns/{id}` — VIEWER. Detail incl. full evidence + review trail. 200/404.
### 13.3 `GET /api/v1/patterns/{id}/evidence` — VIEWER. Page of evidence records (each → record id + role). 200/404.
### 13.4 `POST /api/v1/patterns/{id}/review`
- **Purpose**: human review transition. **Auth**: ENGINEER.
- **Request**: `{"action":"ACKNOWLEDGE|CONFIRM|DISMISS","note":"checked base frame, resonance found"}`
- **Status**: 200, 404, 409 `INVALID_TRANSITION`, 422 note required for CONFIRM/DISMISS.
- **Rules**: `CONFIRMED` reachable **only** via this human endpoint (never system-set);
  transition + note audited; dismissed patterns are suppressed from re-detection (fingerprint).
### 13.5 `POST /api/v1/patterns/scan`
- **Purpose**: trigger an async detection run for a plant (normally nightly + post-import).
- **Auth**: ENGINEER. **Response 202**: `{"jobId":"…"}`. 409 `SCAN_ALREADY_RUNNING`.
- **Entities**: `Pattern`, `PatternEvidence`. **Service**: `PatternScanService`, `PatternReviewService`.

---

# 14. WhatsApp (`/integrations/whatsapp`, `/whatsapp`)

### 14.1 `GET /api/v1/integrations/whatsapp/webhook`
- **Purpose**: provider verification challenge (hub.challenge echo). **Auth**: Public (verify token checked). 200/403.
### 14.2 `POST /api/v1/integrations/whatsapp/webhook`
- **Purpose**: receive inbound messages/status callbacks.
- **Auth**: Public + **mandatory signature verification** (X-Hub-Signature-256); unknown sender → polite refusal reply, no data access.
- **Request**: provider payload (opaque; stored verbatim). **Response**: always 200 fast-ack;
  processing is async (provider retries otherwise).
- **Rules**: idempotent on provider message id; audit `WHATSAPP_INBOUND`.
- **Service**: `WhatsAppWebhookService` → `ConversationEngine`. Flow: `docs/14-whatsapp-api.md`.
### 14.3 `GET /api/v1/whatsapp/conversations` — ENGINEER. Monitor active/complete conversations; filters `plantId,status,phone`. 200 page.
### 14.4 `GET /api/v1/whatsapp/conversations/{id}` — ENGINEER. Transcript + extraction state + resulting record id. 200/404.
### 14.5 `POST /api/v1/whatsapp/contacts`
- **Purpose**: register a technician phone → user/plant binding (identity for webhook).
- **Auth**: PLANT_ADMIN. **Request**: `{"userId":"…","phone":"+9198…","plantId":"…"}` → 201; 409 `PHONE_ALREADY_REGISTERED`.
### 14.6 `DELETE /api/v1/whatsapp/contacts/{id}` — PLANT_ADMIN. 204/404. Audited.

---

# 15. Dashboard & Reports (`/dashboard`, `/reports`)

### 15.1 `GET /api/v1/dashboard`
- **Purpose**: one call for the landing screen: KPIs (12.1), downtime trend, top machines,
  plant Pareto, top 3 insights, validation pending count, recent records.
- **Auth**: VIEWER. **200** composite; each numeric section carries `computedBy:"DETERMINISTIC"`;
  each insight carries its `label` (FACT/CALCULATED/HYPOTHESIS). **Service**: `DashboardService`.
### 15.2 `GET /api/v1/dashboard/insights` — VIEWER. Full insight feed (pattern-derived cards + evidence counts + CTA targets). 200.
### 15.3 `POST /api/v1/reports` — **V1-NICE** — ENGINEER. `{"type":"RECORDS_CSV|PLANT_MONTHLY_PDF","params":{…}}` → 202 `{jobId,reportId}`.
### 15.4 `GET /api/v1/reports` — ENGINEER. 200 page. · 15.5 `GET /api/v1/reports/{id}/download` — ENGINEER. 200 stream / 404 / 409 `NOT_READY`.

---

# 16. Jobs, Audit, Notifications, Meta

### 16.1 `GET /api/v1/jobs/{id}`
- **Purpose**: poll any async job (`IMPORT_PIPELINE, PATTERN_SCAN, EMBEDDING_BACKFILL, REPORT`).
- **Auth**: Authenticated (owner or ENGINEER of plant). **200**: `{"id":"…","type":"IMPORT_PIPELINE","status":"PROCESSING","progressPct":64,"error":null,"resultRef":"importJobId=…"}` /404.
### 16.2 `GET /api/v1/jobs` — ENGINEER. Filters `type,status,plantId`. 200 page.
### 16.3 `GET /api/v1/audit-logs`
- **Purpose**: audit trail query. Filters: `plantId, actorId, action, entityType, entityId, dateFrom, dateTo`.
- **Auth**: PLANT_ADMIN. **200** page: `{id,at,actor,action,entityType,entityId,before,after,traceId,ip}`.
- **Rules**: read-only; export via reports. **Service**: `AuditService`.
### 16.4 `GET /api/v1/notifications` — **V1-NICE** — Authenticated. Own unread/all. 200.
### 16.5 `POST /api/v1/notifications/{id}/read` — Authenticated. 204/404.
### 16.6 `GET /actuator/health` (+`/actuator/prometheus` internal) — infra endpoints, outside `/api/v1`; see `docs/19-observability.md`.

---

## Endpoint count

| Group | Endpoints |
|---|---|
| Auth 5 · Users/Roles 7 · Plants/Lines/Settings 10 · Machines 9 · Aliases/Resolution 4 | 35 |
| Failure modes 4 · Parts 5 · Maintenance records 6 | 15 |
| Imports 10 · Validation 10 · Search 2 | 22 |
| Assistant 6 · Analytics 6 · Patterns 5 | 17 |
| WhatsApp 6 · Dashboard/Reports 5 · Jobs/Audit/Notifications 5 | 16 |
| **Total** | **105** (of which 7 are V1-NICE: reports ×3, notifications ×2, feedback, jobs list) |
