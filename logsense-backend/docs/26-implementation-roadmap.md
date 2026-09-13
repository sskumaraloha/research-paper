# 26 — Implementation Roadmap

16 phases; each lists features, APIs, DB work, services, dependencies, testing and a
definition of done (DoD). Phases 1–4 are strictly sequential; later phases parallelize where
dependencies allow. Flyway migrations are numbered per phase.

| # | Phase | Depends on |
|---|---|---|
| 1 | Project foundation | — |
| 2 | Authentication & users | 1 |
| 3 | Plant + machine master | 2 |
| 4 | Maintenance records | 3 |
| 5 | File ingestion (upload→parse→raw) | 4 |
| 6 | Normalization + machine resolution | 5 |
| 7 | Validation | 6 |
| 8 | Search | 4 (embeddings after 6) |
| 9 | Analytics | 4 |
| 10 | AI assistant | 8, 9 |
| 11 | Pattern detection | 9 |
| 12 | WhatsApp | 6 |
| 13 | Reports + notifications (V1-NICE) | 9, 11 |
| 14 | Security hardening | all APIs exist |
| 15 | Testing completion & performance | 14 |
| 16 | Deployment | 15 |

---

**Phase 1 — Project foundation**
Features: skeleton app, common kernel, error envelope, trace filter, pagination, jobs table,
audit skeleton, OpenAPI wiring, Docker-compose dev stack (Postgres+pgvector, MinIO).
APIs: `GET /jobs/{id}`, actuator health. DB: `V1__core` (jobs, audit_logs). Services:
JobService, AuditService, GlobalExceptionHandler. Testing: envelope contract, Testcontainers
bootstrap, ArchUnit rules. **DoD**: app boots via compose; a fake async job is pollable;
errors match doc 17; CI green.

**Phase 2 — Authentication & users**
APIs: auth 5 + users 7. DB: `V2__identity` (users, user_plant_roles, refresh_tokens).
Services: AuthService, JwtService, PlantAccessService, UserService. Testing: token lifecycle,
role matrix scaffold, rate-limit on login. **DoD**: doc-16 §2 fully working; audit rows for
every auth/user action.

**Phase 3 — Plant + machine master**
APIs: plants/lines/settings 10 + machines CRUD (4.1–4.5) + failure modes 4 + parts CRUD
(6.5–6.8). DB: `V3__masterdata` (plants, lines, plant_settings, machines, failure_modes,
parts). Services: Plant/Line/Settings/Machine/Part services. Testing: scoping (404 semantics),
uniqueness conflicts. **DoD**: an admin can build the doc-25 hierarchy end-to-end via API.

**Phase 4 — Maintenance records**
APIs: 7.1–7.6, machines 4.6 (history), seed loader. DB: `V4__records`
(maintenance_records, record_parts, raw_records [MANUAL path]). Services:
MaintenanceRecordService, RecordProvenanceService; events MaintenanceRecordCreated/Updated.
Testing: manual entry → provenance chain; correction audit. **DoD**: doc-25 seed loads;
history endpoints return it; every record has a raw ancestor.

**Phase 5 — File ingestion**
APIs: imports 8.1–8.5, 8.8, 8.10. DB: `V5__ingestion` (import_jobs, import_files, raw_records
extensions). Services: ImportService, parsers, ColumnMappingService, DuplicateDetector,
StorageClient (+local/S3). Testing: golden files parse to expected raw counts; duplicate-file
409; XXE-safe. **DoD**: 1,842-row fixture uploads, parses, previews, maps; original
downloadable; nothing normalized yet.

**Phase 6 — Normalization + machine resolution**
APIs: 8.6 process, 8.7 cancel, 8.9 retry; resolution 5.1–5.4. DB: `V6__resolution`
(staged_records, machine_aliases). Services: NormalizationService (+LlmClient adapter),
MachineResolverService, AliasService, ImportPipelineOrchestrator. Testing: resolver golden
set; pipeline resume/cancel/retry; fake-LLM batch extraction; confidence routing. **DoD**:
fixture import completes with exactly 1,747 auto / 43 review / 52 skipped; LLM outage pauses
& resumes.

**Phase 7 — Validation**
APIs: validation 9.1–9.10. DB: `V7__validation` (validation_items, validation_actions).
Services: ValidationService, AliasGroupService. Testing: concurrency 409, bulk partial
success, alias-map creates 37 records + alias + re-resolution of other imports. **DoD**: the
doc-04 Workflow 3 demo runs end-to-end.

**Phase 8 — Search**
APIs: search 10.1–10.2. DB: `V8__search` (record_embeddings, tsv/trigram indexes). Services:
SearchService, QueryExpansionService, EmbeddingService (+EmbeddingClient, backfill job).
Testing: doc-08 query table; plant isolation; degraded (no-embedding) mode. **DoD**: all six
example queries return expected hits with why[].

**Phase 9 — Analytics**
APIs: analytics 12.1–12.6, machines 4.7 stats, parts 6.9 usage, dashboard 15.1. Services:
AnalyticsService, KpiCalculator, MachineStatsService, PartUsageService, DashboardService.
Testing: fixture-exact numbers (18.7 h / 7 records etc.); caching invalidation. **DoD**:
dashboard call reproduces the prototype's numbers from the seed.

**Phase 10 — AI assistant**
APIs: assistant 11.1–11.6. DB: `V9__assistant` (ai_conversations/messages/citations).
Services: full doc-07 pipeline incl. NumericGuardrail, CitationBuilder. Testing: doc-24 §6
grounding suite; golden Hinglish/Devanagari questions with fake LLM. **DoD**: Workflow 5
passes; guardrail metrics wired; 424 degradation correct.

**Phase 11 — Pattern detection**
APIs: patterns 13.1–13.5, machines 4.9, dashboard 15.2. DB: `V10__patterns` (patterns,
pattern_evidence + CONFIRMED trigger). Services: PatternScanService + 5 detectors,
PatternReviewService; scheduler + event triggers. Testing: detector math fixtures; human-only
CONFIRM; fingerprint dedupe/dismiss. **DoD**: seed yields the four expected DETECTED patterns;
new record triggers targeted re-scan.

**Phase 12 — WhatsApp**
APIs: whatsapp 14.1–14.6. DB: `V11__whatsapp` (contacts, conversations, messages). Services:
webhook service, ConversationEngine, send service, WhatsAppClient adapter (+ optional
transcription). Testing: doc-24 §8 scripted flows; signature/idempotency. **DoD**: sandbox
number completes the record-#2048 journey; recurrence note fires.

**Phase 13 — Reports + notifications (V1-NICE)**
APIs: 15.3–15.5, 16.4–16.5. DB: `V12__nice` (reports, notifications). **DoD**: CSV export of
filtered records; event-driven in-app notices. *Skippable for first release.*

**Phase 14 — Security hardening**
Rate limiting everywhere, CORS lockdown, header set, secret handling review, dependency scan,
pen-test checklist (upload abuse, IDOR sweep via role-matrix tests), audit coverage
verification. **DoD**: doc-16 checklist 100 %; automated role-matrix suite green.

**Phase 15 — Testing completion & performance**
Fill coverage gates, E2E suite, Gatling smoke on 10⁶-record dataset, slow-query review,
index tuning. **DoD**: doc-24 targets met; CI pipeline < 20 min.

**Phase 16 — Deployment**
Dockerfile (distroless), compose/prod profile, env-var matrix (README), Flyway on-boot
strategy, backup/restore runbook, log/metric shipping, staging + prod cutover with seed-less
prod init. **DoD**: one-command staging deploy; restore drill executed; monitoring alerts live.
