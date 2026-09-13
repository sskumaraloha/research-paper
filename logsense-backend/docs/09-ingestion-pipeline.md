# 09 — Data Ingestion Pipeline

From "here is our messy Excel" to approved, searchable maintenance records — without ever
destroying the original data (Rule 5).

## 1. Pipeline stages & the API for each

```
STAGE                     PERSISTED AS                 API
─────                     ────────────                 ───
1 File upload             import_jobs + import_files   POST /imports
2 File validation         (sync checks)                (part of upload: 413/415/409)
3 File parsing            raw_records (verbatim)       auto after upload → GET /imports/{id}
4 Column mapping          import_jobs.column_mapping   GET /imports/{id}/preview · PUT /imports/{id}/mapping
5 Normalization (LLM)     staged_records               POST /imports/{id}/process (starts 5–8)
6 Machine resolution      staged_records.machineId
7 Part & failure-mode res staged_records.extracted
8 Routing                 AUTO_APPROVED → maintenance_records
                          PENDING_REVIEW → validation_items
                          SKIPPED/FAILED stay on raw/staged rows
9 Validation              validation_items             /validation/** (doc 10)
10 Approval → final record maintenance_records         (validation actions)
11 Indexing               tsvector + record_embeddings event-driven (doc 08 §3)
Monitoring                 jobs                        GET /jobs/{id} · GET /imports/{id}/rows
```

## 2. Stage detail

**1–2 Upload & validation (sync)** — multipart ≤ 50 MB; allowed: `xlsx xls csv pdf png jpg jpeg`;
content-type sniffed (not extension-trusted); AV hook point; SHA-256 → duplicate-file 409;
stored immutably at `plants/{plantId}/imports/{jobId}/{filename}` (object storage / volume).

**3 Parsing (async, starts immediately)**
- xlsx/xls: per sheet, header detection (first row with ≥2 non-empty distinct cells), each data
  row → `raw_records{payload: {col:value}, text: joined, source_ref:{sheet,row}}`.
- csv: delimiter sniffing, same row model.
- pdf: text layer per page → line-block heuristics; each candidate entry → raw record with
  `source_ref:{page,block}`.
- image/scanned pdf: OCR provider (pluggable) → same as pdf. Low OCR confidence rows marked
  for review automatically.
- Empty/duplicate rows (row_hash = sha256(plant, normalized text, date-ish cells)) →
  status SKIPPED with reason `EMPTY | DUPLICATE_ROW | HEADER | UNPARSEABLE`. Nothing is dropped.
- Job → `PARSED` with `counts.detected/parsed/skipped`.

**4 Column mapping** — auto-suggestion by header synonyms ("M/c", "Machine", "Equip" →
MACHINE_TEXT; "Hrs", "Downtime", "Time Lost" → DOWNTIME…) + cell-content heuristics (date
column detection). Targets: `RECORD_DATE, MACHINE_TEXT, DESCRIPTION, ACTION, DOWNTIME,
TECHNICIAN, PART, IGNORE`. Minimum viable mapping: MACHINE_TEXT + DESCRIPTION (or one combined
free-text column). Single-column registers (pure prose) are legal: everything is DESCRIPTION
and the LLM extracts the rest. Mapping is per job (per sheet override allowed). Job may enter
`MAPPING_REQUIRED` if auto-confidence is low.

**5 Normalization** — batched LLM extraction (doc 07 §4): date parsing with the job's
`date_format` first, LLM fallback for prose dates ("3rd week of March"); shorthand expansion;
downtime extraction ("1.5 hr gaya", "2 ghante" → 2.0). Output → `staged_records` with
per-field confidence.

**6 Machine resolution** — `MachineResolverService` (doc 11) on `machineText`. Unresolved or
below plant threshold → routed to review with `group_key` (enables the "37 × Conv Motor-3"
bulk moment).

**7 Part & failure-mode resolution** — part codes matched against `parts` (case/format
tolerant: "6205zz" = "6205ZZ"); unknown codes flagged `NEW_PART_SUSPECT` (approval can create
the part). Failure mode mapped via taxonomy synonyms; unknown → "Other" + low field confidence.

**8 Routing** — `overall_confidence ≥ plant.auto_approve_threshold` AND no missing critical
field AND no duplicate suspicion (same machine+date+similar text vs existing records) →
maintenance record created immediately (source IMPORT). Otherwise `validation_items` row with
reason. Duplicate suspects always go to review, never silently merged.

## 3. Job lifecycle, progress, partial success

```
import_jobs.status: CREATED → PARSED → (MAPPING_REQUIRED) → PROCESSING → COMPLETED | FAILED | CANCELLED
jobs (generic):     QUEUED → PROCESSING → COMPLETED | FAILED | CANCELLED
```
- Progress = processed rows / parsed rows, updated in batch commits (every 100 rows) —
  restart-safe: pipeline resumes from last un-processed raw record (row status is the cursor).
- **Partial success is the normal outcome**: COMPLETED means "every row reached a terminal
  row-state", not "every row became a record". Counts jsonb reports the breakdown; the demo
  numbers (1,842 detected / 1,790 usable / 52 skipped / 43 review) are exactly this contract.
- FAILED (job-level) only for systemic errors (storage unreadable, LLM hard-down after
  retries); row-level errors accumulate as row status FAILED, retryable via
  `POST /imports/{id}/retry` (re-runs FAILED rows only — idempotent).
- Cancellation: cooperative at row-batch boundary; processed rows keep their state.

## 4. Idempotency & duplicates

| Level | Mechanism |
|---|---|
| File | sha256 unique per plant → 409 DUPLICATE_FILE |
| Row-in-file | row_hash unique per job → SKIPPED(DUPLICATE_ROW) |
| Cross-import record | routing-time duplicate suspect (machine+date+trigram-similar text ≥0.8 vs existing records) → validation reason DUPLICATE_SUSPECT |
| Process call | `Idempotency-Key` header + job status guard (409 ALREADY_PROCESSING) |

## 5. Error reporting

`GET /imports/{id}/rows?status=SKIPPED|FAILED` returns every problem row with its reason and
original text — the engineer can fix the source file and re-import only the residue, or
hand-enter stragglers via manual entry. Import summary is kept forever for auditability.

## 6. File storage (see also doc 03 §8, doc 16)

- Metadata in `import_files`; bytes in object storage (S3-compatible; local volume in dev).
- Originals retained indefinitely (traceability promise); lifecycle rule may move to cold tier.
- Download only via authenticated `GET /imports/{id}/file` (audited); no public URLs;
  content served with `Content-Disposition: attachment` + original content type re-validated.
```
Original file  → import_files (immutable)
Raw rows       → raw_records (immutable)
Processed      → staged_records → maintenance_records
Failed/skipped → raw_records with status + reason (visible, never deleted)
```
