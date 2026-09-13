# 02 — Backend Module Architecture

**Architecture decision: modular monolith** (single Spring Boot 3.x application, Java 21,
PostgreSQL 16 + pgvector). Rationale in `docs/27-v1-vs-future.md` — one plant's data volume
(thousands–low millions of records) does not justify microservices, Kafka, or Elasticsearch.
Modules are package-isolated (`com.logsense.<module>`), communicate through service
interfaces and in-process Spring application events, and are shaped for later extraction.

Legend: **V1** = required for first production version · **V1-NICE** = build if time allows ·
**V1 NOT REQUIRED** = explicitly out of scope with reason.

---

## A. `auth` — Authentication & Authorization — **V1**

| | |
|---|---|
| Responsibility | Login, JWT issue/refresh/revoke, password change, current-user info, method-level RBAC + plant scoping |
| Entities | `User` (shared with `user` module), `RefreshToken` |
| Controllers | `AuthController` |
| Services | `AuthService`, `JwtService`, `PlantAccessService` (central "can user X act on plant Y as role Z" check used by every module) |
| Repositories | `RefreshTokenRepository` |
| Integrations | none |
| Events | `UserLoggedIn` (audit only) |
| Depends on | `user`, `audit` |

## B. `plant` — Plant & Line Management — **V1**

| | |
|---|---|
| Responsibility | CRUD for plants and lines (kind PRODUCTION/UTILITY); plant settings (shifts, operating hours, downtime cost, confidence thresholds) |
| Entities | `Plant`, `Line`, `PlantSettings` |
| Controllers | `PlantController`, `LineController`, `SettingsController` |
| Services | `PlantService`, `LineService`, `PlantSettingsService` |
| Repositories | `PlantRepository`, `LineRepository`, `PlantSettingsRepository` |
| Events | none |
| Depends on | `auth`, `audit` |

*Area sub-hierarchy: V1 NOT REQUIRED (see doc 01 §2).*

## C. `machine` — Machine Master — **V1**

| | |
|---|---|
| Responsibility | Machine CRUD, machine detail composition (stats, history, parts, patterns), health status derivation |
| Entities | `Machine` |
| Controllers | `MachineController` |
| Services | `MachineService`, `MachineStatsService` (deterministic MTTR/MTBF/Pareto/downtime per machine) |
| Repositories | `MachineRepository` |
| Events | consumes `MaintenanceRecordCreated` (invalidate cached stats) |
| Depends on | `plant`, `maintenance` (read), `resolution` |

## D. `resolution` — Machine Alias / Resolution — **V1**

| | |
|---|---|
| Responsibility | Resolve free text ("Conv Motor-3", "L3 conv", "कन्वेयर मोटर") to a machine with a confidence score; store aliases; learn from validation corrections |
| Entities | `MachineAlias` |
| Controllers | `MachineAliasController`, `ResolutionController` (debug/resolve endpoint) |
| Services | `MachineResolverService` (exact → alias → normalized-token fuzzy → embedding similarity), `AliasService` |
| Repositories | `MachineAliasRepository` |
| Events | `MachineAliasCreated` (producer); consumed by ingestion to re-resolve pending rows |
| Depends on | `machine`, `search` (embedding client), `audit` |

## E. `maintenance` — Maintenance Records — **V1**

| | |
|---|---|
| Responsibility | The system of record: approved maintenance records, failure-mode taxonomy, parts join, provenance ("View Source"), manual entry, audited corrections, soft delete |
| Entities | `MaintenanceRecord`, `RecordPart`, `FailureMode`, `Part` (Part owned here, exposed via `parts` module services) |
| Controllers | `MaintenanceRecordController`, `FailureModeController` |
| Services | `MaintenanceRecordService`, `RecordProvenanceService` |
| Repositories | `MaintenanceRecordRepository`, `RecordPartRepository`, `FailureModeRepository` |
| Events | `MaintenanceRecordCreated`, `MaintenanceRecordUpdated` (producers) |
| Depends on | `machine`, `resolution`, `audit` |

## F. `ingestion` — Data Ingestion — **V1**

| | |
|---|---|
| Responsibility | File upload/validation/storage, parsing (xlsx/csv/pdf/image via OCR), raw-record persistence, column mapping, import-job orchestration, progress, retry, duplicate detection |
| Entities | `ImportJob`, `ImportFile`, `RawRecord` |
| Controllers | `ImportController` |
| Services | `ImportService`, `FileParserService` (per-format parsers), `ColumnMappingService`, `ImportPipelineOrchestrator` (async), `DuplicateDetector` |
| Repositories | `ImportJobRepository`, `ImportFileRepository`, `RawRecordRepository` |
| Integrations | Object storage (S3-compatible or local volume), OCR provider (for scans — pluggable) |
| Events | `ImportCompleted` (producer) → consumed by `pattern` (schedule scan) and `notification` |
| Depends on | `normalization`, `resolution`, `validation`, `jobs`, `audit` |

## G. `normalization` — Data Normalization — **V1**

Kept as its **own service package** (not merged into ingestion) because WhatsApp and manual
entry reuse it; it owns the LLM extraction prompt/contract.

| | |
|---|---|
| Responsibility | Raw text → structured staged record: expand shorthand/Hinglish (brng→bearing, "2 ghante"→2.0h), extract date/machine-text/failure-mode/action/parts/downtime/technician with per-field confidence |
| Entities | `StagedRecord` |
| Controllers | none (internal; invoked by ingestion/whatsapp/manual entry) |
| Services | `NormalizationService`, `ExtractionPromptBuilder`, `ConfidenceScorer` |
| Repositories | `StagedRecordRepository` |
| Integrations | LLM provider (Claude API by default; pluggable `LlmClient`) |
| Events | none (pipeline-internal) |
| Depends on | `resolution`, `maintenance` (taxonomy lookup) |

## H. `validation` — Validation Queue — **V1**

| | |
|---|---|
| Responsibility | Human review of low-confidence staged records: queue, detail, approve/edit/reject, bulk operations, alias-group mapping, full review history |
| Entities | `ValidationItem`, `ValidationAction` |
| Controllers | `ValidationController` |
| Services | `ValidationService`, `AliasGroupService` (groups items sharing an unresolved machine string) |
| Repositories | `ValidationItemRepository`, `ValidationActionRepository` |
| Events | `ValidationCompleted` (producer, per item/bulk) |
| Depends on | `normalization`, `maintenance`, `resolution`, `audit` |

## I. `search` — Search — **V1**

| | |
|---|---|
| Responsibility | Hybrid search over records/machines/parts/patterns: keyword (Postgres FTS + trigram), synonym/shorthand expansion, alias-aware, semantic (pgvector), merged ranking with "why matched" explanations |
| Entities | `RecordEmbedding` |
| Controllers | `SearchController` |
| Services | `SearchService`, `QueryExpansionService` (synonym dictionary), `EmbeddingService` (+ async backfill) |
| Repositories | `RecordEmbeddingRepository` (pgvector) |
| Integrations | Embedding provider (pluggable `EmbeddingClient`) |
| Events | consumes `MaintenanceRecordCreated` → embed & index |
| Depends on | `maintenance`, `resolution` |

## J. `assistant` — AI Assistant — **V1**

| | |
|---|---|
| Responsibility | Conversational Q&A: query understanding, tool orchestration (deterministic stats + retrieval), grounded answer composition, hypothesis labelling, citations, conversations |
| Entities | `AiConversation`, `AiMessage`, `AiCitation` |
| Controllers | `AssistantController` |
| Services | `AssistantService`, `QueryUnderstandingService`, `AnswerComposerService`, `CitationBuilder`, `AssistantToolExecutor` (whitelisted deterministic tools only) |
| Repositories | `AiConversationRepository`, `AiMessageRepository`, `AiCitationRepository` |
| Integrations | LLM provider (`LlmClient`) |
| Events | none |
| Depends on | `search`, `analytics`, `resolution`, `parts`, `pattern`, `maintenance` |

## K. `analytics` — Analytics — **V1**

| | |
|---|---|
| Responsibility | All deterministic statistics: KPIs (MTTR/MTBF/downtime/breakdowns + deltas), downtime trend, failure Pareto, top machines, parts consumption, line summaries. Single source of numeric truth for dashboard AND assistant |
| Entities | none (reads `maintenance`) |
| Controllers | `AnalyticsController` |
| Services | `AnalyticsService`, `KpiCalculator` |
| Repositories | native/JPQL aggregate queries via `MaintenanceRecordRepository` |
| Events | none |
| Depends on | `maintenance`, `plant` |

## L. `pattern` — Pattern Detection — **V1**

| | |
|---|---|
| Responsibility | Deterministic detectors (recurrence interval, temporary-fix repeat, cross-machine failure, part concentration, downtime hotspot), evidence linking, FACT/HYPOTHESIS labelling, human review workflow |
| Entities | `Pattern`, `PatternEvidence` |
| Controllers | `PatternController` |
| Services | `PatternScanService` (scheduled/async), detectors (strategy list), `PatternReviewService` |
| Repositories | `PatternRepository`, `PatternEvidenceRepository` |
| Events | `PatternDetected` (producer) → `notification`; consumes `ImportCompleted`, `MaintenanceRecordCreated` |
| Depends on | `maintenance`, `analytics`, `jobs` |

## M. `parts` — Spare Parts — **V1**

| | |
|---|---|
| Responsibility | Part catalog CRUD, usage analysis (machines using a part, replacement counts, share by line/machine, timeline, avg interval) derived from record_parts |
| Entities | uses `Part`, `RecordPart` (owned by `maintenance`) |
| Controllers | `PartController` |
| Services | `PartService`, `PartUsageService` |
| Repositories | `PartRepository` |
| Depends on | `maintenance` |

## N. `whatsapp` — WhatsApp Integration — **V1**

| | |
|---|---|
| Responsibility | Webhook (verify + receive), signature validation, technician identity by phone, conversation state machine, missing-field prompts, confirmation, record creation via normalization path, outbound replies; voice-note transcription hook |
| Entities | `WhatsAppContact`, `WhatsAppConversation`, `WhatsAppMessage` |
| Controllers | `WhatsAppWebhookController`, `WhatsAppAdminController` |
| Services | `WhatsAppWebhookService`, `ConversationEngine`, `WhatsAppSendService` |
| Repositories | `WhatsAppContactRepository`, `WhatsAppConversationRepository`, `WhatsAppMessageRepository` |
| Integrations | WhatsApp Business API provider (Meta Cloud API or BSP; pluggable `WhatsAppClient`), transcription provider (voice, pluggable) |
| Events | `WhatsAppRecordConfirmed` (producer) |
| Depends on | `normalization`, `maintenance`, `resolution`, `user` |

## O. `notification` — Notifications — **V1-NICE**

In-app notifications only (validation backlog, pattern detected, import finished). No email/
SMS/push infrastructure in V1 — the dashboard already surfaces the same items, so this is a
convenience layer.

| Entities | `Notification` · Controller `NotificationController` · Service `NotificationService` · consumes `ImportCompleted`, `PatternDetected`, `ValidationCompleted` |

## P. `dashboard` — Dashboard — **V1**

Thin composition module: one aggregate endpoint assembling analytics KPIs, top insights,
validation counts, recent records. No entities of its own.

| Controllers | `DashboardController` · Services | `DashboardService` · Depends on `analytics`, `pattern`, `validation`, `maintenance` |

## Q. `report` — Reports — **V1-NICE**

Async generation of downloadable exports (CSV of filtered records; monthly plant summary
PDF). Justified by "Dashboard & Reports" module in the PDF, but the dashboard covers the
demo need — schedule after core V1. Uses the generic `jobs` mechanism + object storage.

## R. `audit` — Audit Logs — **V1**

| | |
|---|---|
| Responsibility | Append-only audit of security-relevant and data-mutating actions (who/what/when/before-after), query API for admins |
| Entities | `AuditLog` |
| Controllers | `AuditLogController` |
| Services | `AuditService` (invoked via AOP annotation + explicit calls) |
| Repositories | `AuditLogRepository` |
| Depends on | none (everything depends on it) |

## S. `user` — Admin / User Management — **V1**

| | |
|---|---|
| Responsibility | User CRUD, activation, per-plant role assignment, technician phone registration |
| Entities | `User`, `UserPlantRole` |
| Controllers | `UserController`, `RoleController` (static role list) |
| Services | `UserService` |
| Repositories | `UserRepository`, `UserPlantRoleRepository` |
| Depends on | `auth`, `audit` |

## T. `config` / `common` / `jobs` — System plumbing — **V1**

- `config`: Spring configuration (security, async executors, OpenAPI, CORS, storage, LLM
  clients). No REST surface except actuator.
- `common`: error model, `ApiError`, pagination DTOs, ID types, `TraceId` filter, base
  entity (audit columns, soft delete), exception hierarchy.
- `jobs`: generic async job table + `GET /jobs/{id}` polling for ingestion, pattern scans,
  embedding backfill, report generation. States: `QUEUED → PROCESSING → COMPLETED | FAILED | CANCELLED`.

---

## Explicitly not modules in V1

| Candidate | Verdict | Reason |
|---|---|---|
| Kafka / external message broker | **V1 NOT REQUIRED** | In-process Spring events + DB job table suffice at V1 volume; interfaces kept broker-shaped for future swap |
| Elasticsearch | **V1 NOT REQUIRED** | Postgres FTS + pg_trgm + pgvector cover keyword/fuzzy/semantic at this scale |
| Workflow engine (Camunda etc.) | **V1 NOT REQUIRED** | Validation and WhatsApp flows are simple explicit state machines |
| SAP write-back / CMMS sync | **FUTURE** | PDF explicitly avoids SAP changes; V1 only ingests exports |
| Multi-tenant org layer above Plant | **FUTURE** | Single-org, multi-plant is enough for V1 deployments |

## Module dependency sketch

```
common/config/jobs/audit  ← used by all
auth ← user
plant ← auth
machine ← plant
resolution ← machine, search(embeddings)
maintenance ← machine, resolution
ingestion → normalization → resolution → validation → maintenance
search ← maintenance
analytics ← maintenance
pattern ← maintenance, analytics
parts ← maintenance
assistant ← search, analytics, parts, pattern, resolution
whatsapp → normalization → maintenance
dashboard ← analytics, pattern, validation
```
No cycles: `ingestion` never calls `assistant`; `assistant` is read-only over other modules.
