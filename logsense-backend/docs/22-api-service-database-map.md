# 22 — API → Service → Database Map

Request-path traces for the load-bearing endpoints (representative for their families; every
other endpoint in doc 03 follows the same controller→service→repository discipline).

## Auth

```
POST /auth/login
 → AuthController
 → AuthService.login()                      [BCrypt verify, rotate refresh]
 → UserRepository.findByEmail / RefreshTokenRepository.save
 → users, refresh_tokens                    (+ audit_logs via AuditService)
```

## Machine detail family

```
GET /machines/{id}/stats
 → MachineController
 → PlantAccessService.check(VIEWER)
 → MachineStatsService.stats(id, range)     [pure SQL aggregates]
 → MaintenanceRecordRepository (native aggregate queries)
 → maintenance_records (+ failure_modes join)
```
```
GET /maintenance-records/{id}/source
 → MaintenanceRecordController → RecordProvenanceService
 → MaintenanceRecordRepository → RawRecordRepository → StagedRecordRepository → ValidationItemRepository
 → maintenance_records → raw_records → staged_records → validation_items
```

## Ingestion

```
POST /imports (multipart)
 → ImportController → ImportService.create()
 → StorageClient.put (object storage) · ImportFileRepository · ImportJobRepository
 → import_files, import_jobs                → async parse task → RawRecordRepository → raw_records

POST /imports/{id}/process
 → ImportController → ImportService.startPipeline() → JobService.enqueue(IMPORT_PIPELINE)
 → jobs                                     [202 returns jobId]
 (worker) ImportPipelineOrchestrator
   → NormalizationService → LlmClient (batch extract) → StagedRecordRepository → staged_records
   → MachineResolverService → machine_aliases / machines (trigram) / record_embeddings(names)
   → routing: MaintenanceRecordService.createFromStaged → maintenance_records, record_parts
              or ValidationService.enqueue → validation_items
   → ImportJobRepository (progress/counts) → import_jobs
   → publish ImportCompleted → PatternScanService, NotificationService
```

## Validation

```
POST /validation/alias-groups/map
 → ValidationController → AliasGroupService.map(groupKey, machineId)
   → ValidationItemRepository.lockOpenByGroupKey (status guard)
   → per item: StagedRecordRepository.update → MaintenanceRecordService.createFromStaged
   → AliasService.create (machine_aliases) → publish MachineAliasCreated
   → ValidationActionRepository (BULK_MAP rows)
 → validation_items, validation_actions, staged_records, maintenance_records, machine_aliases, audit_logs
```

## Search

```
GET /search/records?q=brng noise conveyor
 → SearchController → SearchService.searchRecords()
   → QueryExpansionService (dictionary)                     [no DB]
   → MachineResolverService.resolve (boost context)         → machine_aliases, machines
   → keyword leg:  MaintenanceRecordRepository.ftsQuery     → maintenance_records (GIN tsv, trigram)
   → semantic leg: EmbeddingClient.embed(q) → RecordEmbeddingRepository.nearest → record_embeddings (HNSW)
   → fusion + why[] assembly (in service)
```

## AI assistant (full trace)

```
POST /assistant/query
 → AssistantController
 → AssistantService
    1 QueryUnderstandingService ──→ LlmClient (schema-constrained)         [no DB]
    2 AssistantToolExecutor (deterministic only)
        resolve_machine  → MachineResolverService → machines, machine_aliases
        machine_stats    → MachineStatsService    → maintenance_records
        stat_query       → AnalyticsService       → maintenance_records
        search_records   → SearchService          → maintenance_records, record_embeddings
        part_usage       → PartUsageService       → parts, record_parts, maintenance_records
        pattern_lookup   → PatternRepository      → patterns, pattern_evidence
    3 AnswerComposerService ──→ LlmClient (tool results injected)
    4 NumericGuardrail (post-validate numerals & labels)                   [no DB]
    5 CitationBuilder → AiCitationRepository → ai_citations
    6 persist → AiConversationRepository, AiMessageRepository → ai_conversations, ai_messages
 → response blocks + citations
```

## Analytics / dashboard

```
GET /analytics/kpis            → AnalyticsController → KpiCalculator → maintenance_records (window + previous window aggregates)
GET /dashboard                 → DashboardController → DashboardService
                                 → AnalyticsService (SQL) + PatternRepository + ValidationItemRepository count
                                 → maintenance_records, patterns, validation_items   [cached 60 s]
```

## Patterns

```
POST /patterns/scan → PatternController → PatternScanService → JobService(jobs)
 (worker) detectors → MaintenanceRecordRepository aggregates
        → PatternRepository upsert by fingerprint → patterns
        → PatternEvidenceRepository → pattern_evidence
        → publish PatternDetected
POST /patterns/{id}/review → PatternReviewService → patterns (status; trigger enforces human CONFIRM) + audit_logs
```

## WhatsApp

```
POST /integrations/whatsapp/webhook
 → WhatsAppWebhookController (fast 200)
 → WhatsAppWebhookService: SignatureVerifier → WhatsAppMessageRepository (idempotent insert)
 → whatsapp_messages → async ConversationEngine
    → WhatsAppContactRepository (identity) → whatsapp_contacts
    → NormalizationService → LlmClient → staged_records (draft)
    → MachineResolverService → machine_aliases/machines
    → state updates → whatsapp_conversations
    → on confirm: MaintenanceRecordService → maintenance_records (+raw_records source_type=WHATSAPP)
    → WhatsAppSendService → WhatsAppClient (outbound ack)
```

## Jobs & audit

```
GET /jobs/{id}    → JobController → JobService → jobs
GET /audit-logs   → AuditLogController → AuditService.query → audit_logs (partitioned)
```

## Invariants visible in every trace

1. Controller never touches a repository; service always runs `PlantAccessService` first.
2. Numbers originate in repositories/SQL (MachineStats/Analytics/PartUsage) — the LLM appears
   only in steps that transform *language*, and its output passes NumericGuardrail.
3. Every write path ends in audit_logs (via AOP or explicit call) and, where relevant, an
   AFTER_COMMIT event (doc 18).
```
