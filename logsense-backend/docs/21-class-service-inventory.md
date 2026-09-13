# 21 — Class / Service Inventory (size estimate before implementation)

Counts are for **V1 including V1-NICE** (report/notification rows marked ◇ are deferrable).
Format: `Class — responsibility — module — key dependencies`.

## Controllers — 20 (18 core + 2 ◇)

| Class | Responsibility | Module |
|---|---|---|
| AuthController | login/refresh/logout/me/change-password | auth |
| UserController / RoleController | user admin / role catalog | user |
| PlantController / LineController / SettingsController | hierarchy + plant config | plant |
| MachineController | machine CRUD, history, stats, parts, patterns views | machine |
| MachineAliasController / ResolutionController | alias CRUD / resolve debug | resolution |
| MaintenanceRecordController / FailureModeController | records + taxonomy | maintenance |
| PartController | parts + usage | parts |
| ImportController | upload→process→rows→file | ingestion |
| ValidationController | queue/summary/item actions/bulk/alias-map | validation |
| SearchController | omni + record search | search |
| AssistantController | query/conversations/citations/feedback | assistant |
| AnalyticsController | kpis/trend/pareto/top/parts/lines | analytics |
| PatternController | list/detail/evidence/review/scan | pattern |
| WhatsAppWebhookController / WhatsAppAdminController | webhook / monitoring+contacts | whatsapp |
| DashboardController | composite + insights | dashboard |
| JobController / AuditLogController | job polling / audit query | jobs, audit |
| ◇ ReportController · ◇ NotificationController | reports / notifications | report, notification |

## Services — 36 (33 core + 3 ◇)

| Class | Responsibility | Module | Depends on |
|---|---|---|---|
| AuthService | credential auth, token lifecycle | auth | UserRepository, JwtService, RefreshTokenRepository, AuditService |
| JwtService | sign/verify/claims | auth | config |
| PlantAccessService | central plant-scope RBAC check | auth | UserPlantRoleRepository (cached) |
| UserService | user admin, role assignment | user | UserRepository, AuditService |
| PlantService / LineService / PlantSettingsService | hierarchy + config | plant | repos, AuditService |
| MachineService | CRUD + list aggregates + derived status | machine | MachineRepository, MaintenanceRecordRepository |
| MachineStatsService | MTTR/MTBF/Pareto/downtime per machine (SQL) | machine | MaintenanceRecordRepository |
| MachineResolverService | text→machine cascade | resolution | AliasRepo, MachineRepo, EmbeddingClient, TextNormalizer |
| AliasService | alias CRUD + learning + conflict rules | resolution | MachineAliasRepository, events |
| MaintenanceRecordService | record lifecycle, manual entry, corrections | maintenance | repos, NormalizationService, AuditService, events |
| RecordProvenanceService | source-drawer assembly | maintenance | RawRecordRepo, StagedRecordRepo, ValidationItemRepo |
| PartService / PartUsageService | catalog / usage analytics (SQL) | parts | PartRepository, RecordPartRepository |
| ImportService | job CRUD, upload, preview, mapping, rows, download | ingestion | repos, StorageClient, FileParser |
| FileParser (port) + XlsxParser/CsvParser/PdfParser/OcrParser | format parsing → raw records | ingestion | poi/csv/pdfbox, OCR adapter |
| ColumnMappingService | header suggestion + validation | ingestion | — |
| DuplicateDetector | file/row/record duplicate logic | ingestion | repos |
| ImportPipelineOrchestrator | async stage machine, progress, retry, cancel | ingestion | NormalizationService, MachineResolverService, ValidationService, JobService |
| NormalizationService | LLM extraction, date/downtime parsing | normalization | LlmClient, TextNormalizer, ConfidenceScorer |
| ExtractionPromptBuilder / ConfidenceScorer | prompt contract / score calibration | normalization | — |
| ValidationService | queue, actions, bulk, concurrency | validation | repos, MaintenanceRecordService, AuditService |
| AliasGroupService | group aggregation + bulk map | validation | ValidationItemRepo, AliasService |
| SearchService | hybrid query, fusion, why[] | search | FTS/trigram queries, RecordEmbeddingRepository, MachineResolverService |
| QueryExpansionService | shorthand/Devanagari expansion | search | dictionaries |
| EmbeddingService | embed/index/backfill | search | EmbeddingClient, RecordEmbeddingRepository, JobService |
| AssistantService | orchestrates the answer pipeline | assistant | below + AiRepos |
| QueryUnderstandingService | LLM intent+entities (schema-constrained) | assistant | LlmClient |
| AssistantToolExecutor | whitelisted deterministic tools | assistant | MachineStatsService, AnalyticsService, SearchService, PartUsageService, PatternRepository, MachineResolverService |
| AnswerComposerService | LLM composition from tool results | assistant | LlmClient |
| NumericGuardrail | post-validation of numerals/labels | assistant | — |
| CitationBuilder | citation rows from retrieval/coverage | assistant | AiCitationRepository |
| AnalyticsService / KpiCalculator | all plant-level SQL aggregates | analytics | MaintenanceRecordRepository |
| PatternScanService (+5 detectors) | detection runs, fingerprints, evidence | pattern | MaintenanceRecordRepository, PatternRepos, JobService |
| PatternReviewService | review transitions (human-only CONFIRM) | pattern | PatternRepository, AuditService |
| WhatsAppWebhookService | signature, idempotency, routing | whatsapp | SignatureVerifier, repos |
| ConversationEngine | state machine, field prompts, confirm | whatsapp | NormalizationService, MachineResolverService, MaintenanceRecordService, WhatsAppSendService |
| WhatsAppSendService | outbound messages/buttons/retry | whatsapp | WhatsAppClient |
| DashboardService | composite assembly + caching | dashboard | AnalyticsService, PatternRepository, ValidationService |
| JobService / JobRunner | job table + executor bridge | jobs | JobRepository, AsyncConfig |
| AuditService | append + query + AOP aspect | audit | AuditLogRepository |
| ◇ ReportService · ◇ NotificationService · ◇ report generators | exports / in-app notices | report, notification | JobService, StorageClient |

## Repositories — 28

users, user_plant_roles, refresh_tokens, plants, lines, plant_settings, machines,
machine_aliases, failure_modes, parts, maintenance_records, record_parts, import_jobs,
import_files, raw_records, staged_records, validation_items, validation_actions,
record_embeddings, ai_conversations, ai_messages, ai_citations, patterns, pattern_evidence,
whatsapp_contacts, whatsapp_conversations, whatsapp_messages, jobs (+ ◇ notifications, ◇ reports,
audit_logs → 31 with NICE).

## Entities — 30 (matches doc 05 table list 1:1, incl. ◇ notifications/reports)

## DTO groups — 21

Auth, User, Plant/Line/Settings, Machine (+Stats), Alias/Resolve, FailureMode, Part (+Usage),
Record (+Source), Import, Validation, Search, Assistant (blocks/citations), Analytics,
Pattern, WhatsApp, Dashboard, Job, Audit, common Page/Error, ◇ Report, ◇ Notification.
≈ 95–110 record classes total.

## Mappers — ~14 (MapStruct: user, plant, line, machine, alias, failure-mode, part, record,
import, validation, pattern, conversation/message, assistant, audit)

## Validators — Bean Validation on DTOs + 4 custom: `@ValidDateRange`, `@AllowedSortFields`,
`@PlantScoped` (parameter check helper), file-type validator.

## Exceptions — 8 shared (doc 17 hierarchy) + per-module error-code enums (~16 small enums).

## Config classes — 13 (doc 20 `config/` list).

## Integration clients — 5 ports / 6 adapters
LlmClient→AnthropicLlmClient · EmbeddingClient→adapter · WhatsAppClient→MetaCloudApiClient ·
StorageClient→S3StorageClient + LocalFsStorageClient · TranscriptionClient→adapter (optional).

## Event classes — 8 (doc 18 V1 list).

## Totals (approx.)

| Kind | Count |
|---|---|
| Controllers | 20 |
| Services | 36 |
| Repositories | 31 |
| Entities | 30 |
| DTO records | ~100 (21 groups) |
| Mappers | 14 |
| Custom validators | 4 |
| Exception classes/enums | ~24 |
| Config classes | 13 |
| Integration ports/adapters | 11 |
| Event classes | 8 |
| **Java classes total** | **≈ 290–320** |

A focused team implements this in the 16-phase plan (doc 26); nothing here exists without an
endpoint, workflow, or rule that requires it.
