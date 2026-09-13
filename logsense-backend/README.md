# LogSense Maintenance Intelligence — Backend Blueprint

> "Your plant's entire breakdown history, searchable in plain Hindi/English,
> in 4 weeks, without a single sensor."

**Status: this repository folder contains the complete backend architecture & API
specification. No implementation code exists yet.** Everything below is **Designed** unless
explicitly marked otherwise.

| State | Meaning | What's in that state |
|---|---|---|
| **Designed** | fully specified in `docs/` | 19 modules, 105 endpoints, 30 tables, AI/search architecture, security, testing, roadmap |
| **Planned** | scoped but intentionally deferred | V1-NICE items (reports, notifications) and FUTURE list in `docs/27-v1-vs-future.md` |
| **Implemented** | working code | *nothing yet* — implementation starts after this blueprint is approved (see roadmap) |

A working **frontend demo prototype** exists separately in `../logsense-demo/`
(HTML/CSS/JS, mock data) — it defines the UX this backend must serve.

## Problem

Factories hold years of maintenance history in Excel sheets, SAP/CMMS exports, PDFs, scanned
registers and technicians' Hinglish shorthand ("MTR brng noise L3 conv, replcd 6205ZZ…").
The knowledge exists but is unsearchable: diagnosing a breakdown means asking whoever
remembers, and recurring failures hide in plain sight.

## Solution

LogSense ingests that history as-is, normalizes it with an LLM, resolves messy machine names
("Conv Motor-3" → Line 3 Conveyor Motor), routes low-confidence extractions through human
validation, and makes everything searchable and analyzable — plus captures new records through
a WhatsApp conversation. No sensors, no SAP migration, no new forms.

## Core features (V1 — designed)

Ingestion pipeline · LLM normalization with confidence · machine alias resolution & learning ·
validation queue with alias-group bulk mapping · immutable raw-data provenance ("View Source")
· hybrid search (keyword + shorthand/Hinglish expansion + semantic) · grounded AI assistant
with citations and FACT/CALCULATED/HYPOTHESIS labels · deterministic analytics (MTTR, MTBF,
downtime, Pareto, part usage) · pattern detection (recurrence, temporary-fix repeats,
part concentration, hotspots) with human-only confirmation · WhatsApp technician entry ·
dashboard · per-plant RBAC, audit, jobs, observability.

## Architecture (recommended & designed)

**Modular monolith** — Spring Boot 3.x / Java 21, PostgreSQL 16 (+ pgvector, pg_trgm, FTS),
Flyway, S3-compatible object storage, pluggable LLM (Anthropic Claude API default), pluggable
embedding provider, WhatsApp Business API provider. In-process events + DB-backed async jobs —
**no Kafka, no Elasticsearch, no microservices in V1** (rationale: `docs/27-v1-vs-future.md`).

Non-negotiable principles (enforced structurally, see docs 05/07/12):
1. Numbers come from SQL, never from the LLM (NumericGuardrail).
2. Every AI statement is traceable to source records (citations schema).
3. Hypothesis ≠ Fact (wire-format labels + DB trigger: only humans confirm).
4. Low-confidence mappings go through human validation.
5. Raw historical data is never destroyed by normalization.

## Documentation map

| Area | File |
|---|---|
| Product workflows & actors | `docs/01-product-workflow.md` |
| Modules | `docs/02-backend-modules.md` |
| **Full API catalog (105 endpoints)** | `docs/03-api-catalog.md` · quick ref `docs/API-MASTER-INDEX.md` |
| End-to-end API workflows | `docs/04-api-workflows.md` |
| Database (30 tables) & ERD | `docs/05-database-design.md` · `docs/06-entity-relationships.md` |
| AI architecture | `docs/07-ai-architecture.md` |
| Search | `docs/08-search-architecture.md` |
| Ingestion pipeline | `docs/09-ingestion-pipeline.md` |
| Validation · Machine resolution | `docs/10-validation-system.md` · `docs/11-machine-resolution.md` |
| Patterns · Parts · WhatsApp · Dashboard | `docs/12…15` |
| Security · Errors · Events · Observability | `docs/16…19` |
| Spring structure · class inventory · request traces | `docs/20…22` |
| OpenAPI · Testing · Demo data | `docs/23…25` |
| Roadmap (16 phases) · Scope control | `docs/26-implementation-roadmap.md` · `docs/27-v1-vs-future.md` |

## API overview

`/api/v1/{auth, users, roles, plants, lines, machines, machine-aliases, machine-resolution,
failure-modes, parts, maintenance-records, imports, validation, search, assistant, analytics,
patterns, integrations/whatsapp, whatsapp, dashboard, reports, jobs, audit-logs, notifications}`
— uniform pagination (`page,size,sort`), filters (`plantId, machineId, dateFrom, dateTo…`),
error envelope with stable codes and traceId, JWT bearer auth with per-plant roles
(`SUPER_ADMIN, PLANT_ADMIN, ENGINEER, TECHNICIAN, VIEWER`).

## Local development (planned — once implementation starts)

```
Prereqs: JDK 21, Docker
docker compose up -d        # postgres+pgvector, minio
./mvnw spring-boot:run      # profile: dev (loads demo seed, docs/25)
Swagger: http://localhost:8080/swagger-ui
```

### Environment variables (designed contract)

```
DB_URL, DB_USER, DB_PASSWORD
JWT_SECRET, JWT_ACCESS_TTL=15m, JWT_REFRESH_TTL=30d
STORAGE_PROVIDER=s3|local, S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY
LLM_PROVIDER=anthropic, LLM_API_KEY, LLM_MODEL_ANSWER=claude-sonnet-5,
LLM_MODEL_EXTRACT=claude-haiku-4-5-20251001
EMBEDDING_PROVIDER, EMBEDDING_API_KEY, EMBEDDING_MODEL, EMBEDDING_DIM
WHATSAPP_PROVIDER=meta, WHATSAPP_TOKEN, WHATSAPP_PHONE_ID, WHATSAPP_VERIFY_TOKEN, WHATSAPP_APP_SECRET
TRANSCRIPTION_PROVIDER (optional)
CORS_ALLOWED_ORIGINS, RATE_LIMIT_* , LOG_LEVEL
```

## Future integrations & roadmap

FUTURE list (Kafka, Elasticsearch, microservice extraction, SAP sync, predictive models,
SSO…) with explicit triggers: `docs/27-v1-vs-future.md`. Implementation is phased 1→16 in
`docs/26-implementation-roadmap.md`; the first shippable slice is Phases 1–9 (everything up to
analytics/dashboard), with AI, patterns and WhatsApp following in 10–12.

## Git / GitHub

This folder lives inside an existing repository (`sskumaraloha/research-paper`) next to the
frontend prototype. For a standalone implementation repo we recommend the name
**`logsense-backend`** with this same layout (docs/ committed first, code added per phase) —
create it under your own account/organization; no credentials are assumed by this blueprint.

Typical flow from the repo root:
```
git status                       # inspect what changed
git add logsense-backend
git commit -m "docs: LogSense backend architecture & API specification"
git push -u origin <your-branch>
```
