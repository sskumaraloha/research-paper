# 05 — Database Design (PostgreSQL 16)

Single PostgreSQL database with extensions: **pgcrypto** (UUID), **pg_trgm** (fuzzy match),
**vector/pgvector** (embeddings), built-in **tsvector** FTS. Schema managed by Flyway.

## Global conventions

- PK: `id UUID` (UUIDv7, app-generated).
- Audit columns on every business table: `created_at timestamptz NOT NULL`,
  `updated_at timestamptz NOT NULL`, `created_by uuid NULL`, `updated_by uuid NULL`.
- **Soft delete**: `deleted_at timestamptz NULL` on user-manageable entities (users, plants,
  lines, machines, aliases, parts, failure_modes, maintenance_records, ai_conversations).
  Repositories filter `deleted_at IS NULL` by default. Pipeline/audit tables are never deleted.
- **Immutability**: `raw_records`, `audit_logs`, `validation_actions`, `whatsapp_messages`,
  `ai_messages`, `ai_citations` are append-only (no UPDATE except status columns noted).
- Text is `text` (no arbitrary varchar limits) with CHECK length caps where abuse is possible.
- Money/hours: `numeric(8,2)`; percentages computed, never stored.

---

## Tables (30) — with justification

### Identity & access

**`users`** — required: every action is attributed.
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| name | text NOT NULL | |
| email | citext NOT NULL | UNIQUE |
| phone | text NULL | E.164; UNIQUE where not null |
| password_hash | text NOT NULL | BCrypt |
| is_super_admin | boolean NOT NULL default false | global role kept off the per-plant table |
| active | boolean NOT NULL default true | |
| last_login_at | timestamptz | |
| + audit + deleted_at | | |
Indexes: `ux_users_email`, `ux_users_phone (partial WHERE phone IS NOT NULL)`.

**`user_plant_roles`** — required: RBAC is plant-scoped (Rule: plant-level authorization).
`id, user_id FK→users, plant_id FK→plants, role text CHECK (role IN ('PLANT_ADMIN','ENGINEER','TECHNICIAN','VIEWER'))`
UNIQUE `(user_id, plant_id)`. Index on `plant_id`.

**`refresh_tokens`** — required: refresh rotation + revocation.
`id, user_id FK, token_hash text UNIQUE, expires_at, revoked_at NULL, replaced_by uuid NULL, user_agent, ip`.
Index `(user_id, revoked_at)`. Rows pruned by retention job (30 d past expiry).

*(A `roles`/`permissions` pair of tables is **not required** in V1 — the role set is a fixed
enum with hard-coded permission mapping; a dynamic permission matrix is enterprise complexity
the PDF does not ask for.)*

### Plant hierarchy

**`plants`** — `id, code text UNIQUE, name, city, timezone text NOT NULL default 'Asia/Kolkata'` + audit + deleted_at.

**`lines`** — `id, plant_id FK, code, name, kind text CHECK (kind IN ('PRODUCTION','UTILITY'))` + audit + deleted_at.
UNIQUE `(plant_id, code)`. *(No `areas` table — see doc 01 §2.)*

**`plant_settings`** — required: thresholds/config are per plant and referenced by pipeline.
`id, plant_id FK UNIQUE, date_format, shift_config jsonb, operating_hours text,
downtime_cost_per_hour numeric(12,2), auto_approve_threshold numeric(3,2) default 0.90,
alias_suggest_threshold numeric(3,2) default 0.75` + audit.

### Machines & resolution

**`machines`** — `id, line_id FK, code, name, type, criticality text NULL,
commissioned_on date NULL` + audit + deleted_at.
UNIQUE `(line_id, code)`; also enforce plant-wide code uniqueness via trigger/service check.
Indexes: `line_id`; trigram index on `lower(name)` for fuzzy resolution;
generated column `search_tsv tsvector` (name+code+type) with GIN index.
*(Status HEALTHY/ATTENTION/RECURRING is **derived**, not stored — computed from recent
breakdown density + open patterns; storing it would drift.)*

**`machine_aliases`** — required: the resolver's memory; the "Conv Motor-3" fix persists here.
| column | type |
|---|---|
| id | uuid PK |
| machine_id | FK→machines NOT NULL |
| alias | text NOT NULL (as seen) |
| normalized_alias | text NOT NULL (lowercased, shorthand-expanded, squashed) |
| source | text CHECK IN ('SEED','VALIDATION','MANUAL') |
| + audit + deleted_at |
UNIQUE `(plant-scope via machine, normalized_alias)` → implemented as UNIQUE
`(normalized_alias, plant_id)` with denormalized `plant_id` column (avoids cross-plant clashes,
allows the 409 `ALIAS_ALREADY_MAPPED` check in one index probe). Trigram index on `normalized_alias`.

### Taxonomy & parts

**`failure_modes`** — required: canonical Pareto axis. `id, plant_id FK, name, synonyms text[] , sort_order int` + audit + deleted_at. UNIQUE `(plant_id, lower(name))`.

**`parts`** — required: part analytics. `id, plant_id FK, code, name, uom text NULL` + audit + deleted_at. UNIQUE `(plant_id, upper(code))`; trigram on code+name.

### Maintenance core

**`maintenance_records`** — the system of record.
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| machine_id | FK→machines NOT NULL | |
| plant_id | FK denormalized | hot filter on every query |
| record_date | date NOT NULL | |
| kind | text CHECK IN ('BREAKDOWN','PM','INSPECTION') | |
| failure_mode_id | FK→failure_modes NULL | null for pure PM |
| raw_text | text NOT NULL | verbatim copy for display (immutable) |
| action_summary | text NOT NULL | normalized action |
| downtime_hours | numeric(8,2) NULL CHECK (>=0 AND <=720) | |
| technician_name | text NULL | free text (historical names aren't users) |
| technician_user_id | FK→users NULL | set for WhatsApp/manual |
| source_type | text CHECK IN ('IMPORT','WHATSAPP','MANUAL') | |
| raw_record_id | FK→raw_records NOT NULL | **provenance — never null** |
| + audit + deleted_at | | |
Indexes: `(plant_id, record_date desc)`, `(machine_id, record_date desc)`,
`(plant_id, failure_mode_id)`, GIN on `search_tsv` (raw_text + action_summary),
trigram on `raw_text` (shorthand fuzzy).

**`record_parts`** — join with quantity: `id, record_id FK, part_id FK, quantity numeric(8,2) default 1`. UNIQUE `(record_id, part_id)`. Required: powers all part analytics.

*(Separate `Failure` and `Downtime` tables are **not required** in V1: a record has exactly one
primary failure mode and one downtime figure in every PDF example. A multi-failure split is a
future migration, not a V1 cost.)*

### Ingestion pipeline

**`import_jobs`** — `id, plant_id FK, status CHECK IN
('CREATED','PARSED','MAPPING_REQUIRED','PROCESSING','COMPLETED','FAILED','CANCELLED'),
stage text NULL, description, column_mapping jsonb, date_format,
counts jsonb  -- {detected,parsed,skipped,normalized,autoApproved,needsReview,failed}
, started_at, finished_at, error text` + audit. Index `(plant_id, created_at desc)`.

**`import_files`** — `id, import_job_id FK UNIQUE, original_name, content_type, size_bytes,
sha256 text, storage_key text` + audit. UNIQUE `(plant-scope, sha256)` for duplicate-file 409.

**`raw_records`** — **immutable** verbatim rows (Rule 5: raw data never destroyed).
`id, import_job_id FK NULL, plant_id, source_type CHECK IN ('IMPORT','WHATSAPP','MANUAL'),
source_ref jsonb  -- {sheet,row} | {conversationId,messageIds} | {userId}
, payload jsonb  -- original cell values keyed by column
, text text NOT NULL  -- assembled free text
, status CHECK IN ('PARSED','SKIPPED','FAILED','PROCESSED'), skip_reason text NULL, row_hash text`.
Indexes: `(import_job_id,status)`, `ux (import_job_id, row_hash)` for row-level duplicate detection.

**`staged_records`** — normalization output awaiting routing (the validation payload).
`id, raw_record_id FK UNIQUE, plant_id, extracted jsonb  -- {date,machineText,machineId,failureModeId,
 action, partCodes[], downtimeHours, technician, kind}
, field_confidence jsonb, overall_confidence numeric(3,2),
resolution_method text NULL CHECK IN ('EXACT','ALIAS','FUZZY','SEMANTIC','HUMAN'),
status CHECK IN ('EXTRACTED','AUTO_APPROVED','PENDING_REVIEW','APPROVED','REJECTED'),
maintenance_record_id FK NULL  -- set on approval`.
Indexes: `(plant_id,status)`, `(status, lower(extracted->>'machineText'))` for alias grouping.

### Validation

**`validation_items`** — `id, staged_record_id FK UNIQUE, plant_id, reason CHECK IN
('LOW_MACHINE_CONFIDENCE','LOW_FIELD_CONFIDENCE','MISSING_FIELDS','DUPLICATE_SUSPECT'),
group_key text NULL  -- normalized unresolved machine text ("conv motor-3")
, status CHECK IN ('OPEN','RESOLVED'), resolved_at, resolved_by FK→users NULL` + audit.
Indexes: `(plant_id,status)`, `(group_key) WHERE status='OPEN'`.
Justification: separates queue workflow state from extraction data; enables alias grouping.

**`validation_actions`** — append-only review trail.
`id, validation_item_id FK, actor_id FK→users, action CHECK IN
('APPROVE','EDIT_APPROVE','REJECT','BULK_MAP'), before jsonb NULL, after jsonb NULL, note text` + created_at.

### Search

**`record_embeddings`** — `record_id PK FK→maintenance_records, plant_id, embedding vector(1024),
model text, embedded_at`. HNSW index `USING hnsw (embedding vector_cosine_ops)`.
Justification: semantic leg of hybrid search; separate table keeps the hot record table lean
and lets embeddings be re-generated per model without touching records.

### AI assistant

**`ai_conversations`** — `id, user_id FK, plant_id FK, title text` + audit + deleted_at. Index `(user_id, updated_at desc)`.
**`ai_messages`** — `id, conversation_id FK, role CHECK IN ('USER','ASSISTANT'), question text NULL,
blocks jsonb NULL  -- typed, labelled answer blocks
, resolution jsonb NULL, tools_used text[], model text, latency_ms int, tokens_in int, tokens_out int` + created_at.
Justification: reproducibility + cost tracking; blocks as jsonb because block schema is
versioned application contract, not relational data.
**`ai_citations`** — `id, message_id FK, record_id FK→maintenance_records NULL,
raw_record_id FK→raw_records NULL, excerpt text, block_index int`.
CHECK: at least one of record_id/raw_record_id NOT NULL. Justification: Rule 2 traceability —
a citation row is the auditable bridge from an AI sentence to source data.

### Patterns

**`patterns`** — `id, plant_id, machine_id FK NULL (null for cross-machine/line patterns),
type CHECK IN ('RECURRENCE','TEMP_FIX_REPEAT','CROSS_MACHINE','PART_CONCENTRATION','DOWNTIME_HOTSPOT'),
fingerprint text  -- stable hash (type+scope+mode) for dedupe & dismiss-suppression
, fact jsonb  -- metrics: events, avgIntervalDays, spanMonths, downtime share …
, hypothesis_text text NULL, hypothesis_confidence CHECK IN ('POSSIBLE','LIKELY') NULL,
status CHECK IN ('DETECTED','UNDER_REVIEW','CONFIRMED','DISMISSED'),
detected_at, last_evaluated_at, reviewed_by FK NULL, review_note text NULL` + audit.
UNIQUE `(plant_id, fingerprint)`. **DB-level guard**: trigger rejects transition to
CONFIRMED unless `reviewed_by IS NOT NULL` (defense-in-depth for Rule 3).
**`pattern_evidence`** — `id, pattern_id FK, record_id FK→maintenance_records, role text NULL
 (e.g. 'occurrence 3/4'), added_at`. UNIQUE `(pattern_id, record_id)`.

### WhatsApp

**`whatsapp_contacts`** — `id, user_id FK, plant_id FK, phone text` + audit + deleted_at. UNIQUE `(phone)`.
**`whatsapp_conversations`** — `id, contact_id FK, plant_id, state CHECK IN
('NEW','EXTRACTING','AWAITING_FIELD','AWAITING_CONFIRMATION','CONFIRMED','ABANDONED'),
awaiting_field text NULL, staged_record_id FK NULL, maintenance_record_id FK NULL,
last_inbound_at, expires_at` + audit. Index `(contact_id, state)`.
**`whatsapp_messages`** — append-only: `id, conversation_id FK, direction CHECK IN ('IN','OUT'),
provider_message_id text UNIQUE (idempotency), type CHECK IN ('TEXT','VOICE','BUTTON','SYSTEM'),
body text, media_key text NULL, transcript text NULL, payload jsonb, created_at`.

### Platform

**`jobs`** — generic async jobs: `id, type CHECK IN ('IMPORT_PIPELINE','PATTERN_SCAN',
'EMBEDDING_BACKFILL','REPORT'), plant_id NULL, status CHECK IN
('QUEUED','PROCESSING','COMPLETED','FAILED','CANCELLED'), progress_pct int, payload jsonb,
result_ref text, error text, requested_by FK, started_at, finished_at` + created_at.
Index `(status, type)`. Justification: uniform polling contract + at-least-once restart safety.

**`audit_logs`** — append-only: `id, at timestamptz, actor_id FK NULL, plant_id NULL,
action text, entity_type text, entity_id text, before jsonb NULL, after jsonb NULL,
trace_id text, ip text`. Indexes `(plant_id, at desc)`, `(entity_type, entity_id)`.
Monthly partitioning from day one (cheap, avoids repartitioning later).

**`notifications`** *(V1-NICE)* — `id, user_id FK, type, title, body, link text, read_at NULL` + created_at. Index `(user_id, read_at)`.

**`reports`** *(V1-NICE)* — `id, plant_id, type, params jsonb, status, storage_key, requested_by` + audit.

---

## Tables considered and rejected for V1

| Table | Verdict | Why |
|---|---|---|
| `areas` | NOT REQUIRED | Line kind covers Utilities; additive later |
| `roles`, `permissions` | NOT REQUIRED | fixed enum role model |
| `failures` (separate) | NOT REQUIRED | single primary failure mode per record in all PDF examples |
| `downtimes` (separate) | NOT REQUIRED | single downtime figure per record |
| `search_queries` log | FUTURE | analytics nicety; audit_logs covers security need |
| `system_configuration` (global) | MERGED | per-plant `plant_settings` + application properties suffice |

## Retention & growth

- raw_records + import files: retained indefinitely (product promise); files cold-storable.
- refresh_tokens pruned; jobs pruned after 90 d; audit_logs partition-archived after 24 m.
- Expected V1 scale per plant: ~10⁴–10⁶ maintenance records — comfortably Postgres territory;
  all list queries are index-backed and plant-partitionable later if needed.
