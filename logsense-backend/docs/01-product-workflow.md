# 01 — Product Workflow Analysis

Source of truth: `Factory.pdf` — **LogSense Maintenance Intelligence**.
Positioning: *"Your plant's entire breakdown history, searchable in plain Hindi/English, in 4 weeks, without a single sensor."*

LogSense makes existing maintenance history (Excel, CSV, SAP/CMMS exports, PDFs, scanned
registers, technician shorthand, Hinglish notes) searchable and analyzable, and captures new
records conversationally via WhatsApp. It does **not** replace SAP/CMMS and does **not** need
sensors. V1 scope is deliberately limited to what the PDF describes.

---

## 1. Actors

| Actor | Description | Primary usage |
|---|---|---|
| **Plant Head** | Owns plant P&L; consumes KPIs and insights | Dashboard, reports, read-only analytics |
| **Maintenance Manager** | Runs the maintenance function (e.g., "Rajesh Kumar") | Dashboard, analytics, validation, AI assistant, imports |
| **Reliability / Maintenance Engineer** | Investigates failures, validates data | Validation queue, machine history, AI assistant, patterns |
| **Technician** | Shop floor; reports work in Hinglish/shorthand | WhatsApp entry, read machine history |
| **IT Head / System Admin** | Configures users, plants, security | User management, settings, audit |
| **System (LogSense)** | Ingestion pipeline, resolver, pattern scanner, AI assistant | Async jobs, webhooks |
| **External systems** | WhatsApp Business API provider, LLM provider, embedding provider, object storage | Integrations only — no SAP write-back in V1 |

Role model (see `docs/16-security.md`): `SUPER_ADMIN`, `PLANT_ADMIN`, `ENGINEER`, `TECHNICIAN`, `VIEWER`.
Roles are granted **per plant** (except SUPER_ADMIN, which is global).

## 2. Domain hierarchy

```
User (with per-plant role)
 ↓
Plant                      e.g., Demo Pune Manufacturing Plant
 ↓
Line                       e.g., Line 1..4, Utilities (kind = PRODUCTION | UTILITY)
 ↓
Machine                    e.g., Line 3 Conveyor Motor (CONV-L3-MTR-01)
 ↓
Maintenance Records        breakdowns, PM, inspections — each traceable to its raw source
 ↓
Failure modes / Parts / Downtime
 ↓
Search / Analytics / AI / Patterns
```

Notes on scope:
- **Area** (a level between Plant and Line) is `V1 NOT REQUIRED`. The PDF's plants are
  organized as lines plus a "Utilities" group; modeling Utilities as a `Line` of kind
  `UTILITY` covers every example without an extra hierarchy level. Adding Area later is an
  additive migration (nullable `area_id` on `line`).
- A machine belongs to exactly one line; a line to exactly one plant. All data access is
  plant-scoped.

## 3. Maintenance record lifecycle

A maintenance record is the core asset. Two birth paths, one lifecycle:

```
Path A: historical import                Path B: live capture
──────────────────────────               ─────────────────────
File row (raw_record)                    WhatsApp message / manual UI entry
        ↓                                        ↓
Normalization (LLM extraction)           Conversational extraction (LLM)
        ↓                                        ↓
Staged record (+confidence)              Draft staged record
        ↓                                        ↓
confidence ≥ threshold?                  Technician confirms in chat
   yes ↓         no → VALIDATION QUEUE → approve/edit/reject
        ↓                    ↓
        └──────→ MAINTENANCE RECORD (ACTIVE) ←──────┘
                          ↓
              indexed for search + embeddings
                          ↓
        analytics / patterns / AI answers cite it
```

States of a staged record: `EXTRACTED → AUTO_APPROVED | PENDING_REVIEW → APPROVED | REJECTED`.
States of a maintenance record: `ACTIVE → CORRECTED (new version, audited) → DELETED (soft)`.

**Invariant (PDF trust principle):** the raw source text is immutable and never destroyed by
normalization. Every maintenance record keeps a link to its `raw_record` (file/sheet/row or
WhatsApp conversation), which powers "View Source" and AI citations.

## 4. Data ingestion lifecycle

```
Engineer uploads file (xlsx/csv/pdf/image)
 ↓  import_job = CREATED, file stored immutably
Parse (sheet/row or page extraction)      → raw_records persisted verbatim
 ↓  import_job = PARSED (row counts known)
Column mapping (auto-suggested, user-confirmable)
 ↓
Normalize each row (async): expand shorthand/Hinglish, extract
  date, machine text, failure mode, action, parts, downtime, technician
 ↓
Machine resolution (exact → alias → fuzzy/semantic, with confidence)
Part & failure-mode resolution
 ↓
Route: high-confidence → auto-approved records
       low-confidence  → validation queue
       unusable rows   → SKIPPED with reason (kept, reportable)
 ↓  import_job = COMPLETED (detected / usable / skipped / needs-review counts)
Index approved records (keyword + embeddings)
```

Details and APIs: `docs/09-ingestion-pipeline.md`.

## 5. Validation lifecycle

```
Staged record below confidence threshold
 ↓
validation_item (OPEN) — shows original raw text side-by-side with extraction
 ↓
Engineer: APPROVE | EDIT+APPROVE | REJECT       (single or bulk)
Special case: alias group — "37 records use 'Conv Motor-3'"
 ↓  one action maps all 37 + creates machine_alias (resolver learns)
validation_item → RESOLVED; every action recorded in validation_actions (audit)
 ↓
Approved → maintenance record created/updated; Rejected → excluded from index & stats
```

Details: `docs/10-validation-system.md`.

## 6. AI question lifecycle

```
User question (English / Hindi / Hinglish / shorthand)
 ↓
Query understanding (LLM): intent + entities (machine text, part, failure mode, date range)
 ↓
Machine resolution (same resolver as ingestion)
 ↓
Deterministic tools: SQL statistics (downtime, counts, MTTR, Pareto, part usage)
                     + hybrid retrieval of relevant records
 ↓
LLM composes the answer FROM tool outputs only (numbers are injected, never generated)
 ↓
Answer blocks: summary | facts | calculated statistics | patterns | hypothesis (labelled) 
 ↓
Citations: every claim links to maintenance/raw record IDs
```

The LLM never invents operational numbers — see `docs/07-ai-architecture.md` (Rule 1).

## 7. Pattern detection lifecycle

```
Nightly scan (or triggered after large import / new record on a flagged machine)
 ↓
Deterministic detectors over maintenance records:
  recurrence intervals, repeated temporary fixes, cross-machine failure clusters,
  part-consumption concentration, downtime hotspots
 ↓
pattern (status = DETECTED, label = FACT-backed evidence + HYPOTHESIS explanation)
 ↓
Engineer review: ACKNOWLEDGED → CONFIRMED (human only) | DISMISSED
```

The system may propose "POSSIBLE mounting/alignment issue"; **only a human** can move a
pattern to CONFIRMED. Details: `docs/12-pattern-detection.md`.

## 8. WhatsApp entry lifecycle

```
Registered technician sends message ("Line 3 ka conveyor motor band tha, bearing change kiya…")
 ↓  webhook (signature-verified) → whatsapp_message stored
Conversation state machine:
  NEW → EXTRACTING → AWAITING_FIELD (asks e.g. "Downtime kitna tha?")
      → AWAITING_CONFIRMATION (echoes structured summary) → CONFIRMED | ABANDONED
 ↓
On confirm: staged record (source=WHATSAPP) → auto-approve path → maintenance record
 ↓
Reply with record ID; recurrence checks may flag pattern for review
```

Voice notes: media is fetched and sent to a pluggable transcription provider, then follows
the same text path (`docs/14-whatsapp-api.md`). Transcription provider is external.

## 9. User / admin lifecycle

```
SUPER_ADMIN creates plant → PLANT_ADMIN assigned
PLANT_ADMIN invites users → assigns per-plant role → user sets password on first login
User authenticates (JWT + refresh) → plant-scoped access everywhere
Deactivation = soft (login blocked, history retained); all admin actions audit-logged
Technicians additionally registered by phone number for WhatsApp identity
```

## 10. What V1 deliberately does NOT do

- No sensor/IoT data, no SAP write-back, no CMMS replacement (read-only exports in).
- No work-order scheduling/planning module (LogSense is memory + intelligence, not a CMMS).
- No multi-language UI concerns in the backend beyond storing/searching multilingual text.
- No confirmed-root-cause automation, ever (product trust rule).

Full scope table: `docs/27-v1-vs-future.md`.
