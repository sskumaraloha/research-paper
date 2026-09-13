# 20 — Spring Boot Package Structure

Single application `com.logsense` (Spring Boot 3.x, Java 21, Maven). Feature-first packages;
each module keeps its own controller/service/repository/dto/entity/mapper so it can be
extracted later without repackaging. **No implementation code here — structure only.**

```
com.logsense
│
├── LogsenseApplication.java
│
├── common/                          shared kernel (no business logic)
│   ├── api/          ApiError, PageResponse<T>, PageRequestParams, SortWhitelist
│   ├── entity/       BaseEntity (id, audit cols), SoftDeletable
│   ├── exception/    NotFoundException, ConflictException, ValidationFailedException,
│   │                 UpstreamUnavailableException, RateLimitedException, GlobalExceptionHandler
│   ├── util/         TextNormalizer (shorthand/Devanagari dictionary), DateRanges, Ids (UUIDv7)
│   └── trace/        TraceIdFilter, RequestLoggingFilter
│
├── config/                          SecurityConfig, JwtConfig, AsyncConfig (executors),
│                                    OpenApiConfig, CorsConfig, StorageConfig, LlmConfig,
│                                    EmbeddingConfig, WhatsAppConfig, CacheConfig, RateLimitConfig,
│                                    SchedulingConfig, FlywayConfig
│
├── auth/
│   ├── controller/   AuthController
│   ├── service/      AuthService, JwtService, PlantAccessService
│   ├── repository/   RefreshTokenRepository
│   ├── entity/       RefreshToken
│   └── dto/          LoginRequest, TokenResponse, MeResponse, ChangePasswordRequest
│
├── user/
│   ├── controller/   UserController, RoleController
│   ├── service/      UserService
│   ├── repository/   UserRepository, UserPlantRoleRepository
│   ├── entity/       User, UserPlantRole
│   ├── dto/          UserDtos (Create/Update/Response, PlantRoleAssignment)
│   └── mapper/       UserMapper
│
├── plant/
│   ├── controller/   PlantController, LineController, SettingsController
│   ├── service/      PlantService, LineService, PlantSettingsService
│   ├── repository/   PlantRepository, LineRepository, PlantSettingsRepository
│   ├── entity/       Plant, Line, PlantSettings
│   ├── dto/          PlantDtos, LineDtos, SettingsDtos
│   └── mapper/       PlantMapper
│
├── machine/
│   ├── controller/   MachineController
│   ├── service/      MachineService, MachineStatsService
│   ├── repository/   MachineRepository
│   ├── entity/       Machine
│   ├── dto/          MachineDtos (ListRow, Detail, Create, Update, StatsResponse)
│   └── mapper/       MachineMapper
│
├── resolution/
│   ├── controller/   MachineAliasController, ResolutionController
│   ├── service/      MachineResolverService, AliasService
│   ├── repository/   MachineAliasRepository
│   ├── entity/       MachineAlias
│   └── dto/          AliasDtos, ResolveRequest/Response
│
├── maintenance/
│   ├── controller/   MaintenanceRecordController, FailureModeController
│   ├── service/      MaintenanceRecordService, RecordProvenanceService
│   ├── repository/   MaintenanceRecordRepository, RecordPartRepository, FailureModeRepository
│   ├── entity/       MaintenanceRecord, RecordPart, FailureMode
│   ├── dto/          RecordDtos (Summary, Detail, Create, Update, SourceResponse), FailureModeDtos
│   ├── mapper/       RecordMapper
│   └── event/        MaintenanceRecordCreated, MaintenanceRecordUpdated
│
├── parts/
│   ├── controller/   PartController
│   ├── service/      PartService, PartUsageService
│   ├── repository/   PartRepository
│   ├── entity/       Part
│   └── dto/          PartDtos, PartUsageResponse
│
├── ingestion/
│   ├── controller/   ImportController
│   ├── service/      ImportService, ImportPipelineOrchestrator, ColumnMappingService,
│   │                 DuplicateDetector, parser/ (XlsxParser, CsvParser, PdfParser, OcrParser
│   │                 implementing FileParser)
│   ├── repository/   ImportJobRepository, ImportFileRepository, RawRecordRepository
│   ├── entity/       ImportJob, ImportFile, RawRecord
│   ├── dto/          ImportDtos (JobResponse, PreviewResponse, MappingRequest, RowResponse)
│   └── event/        ImportCompleted
│
├── normalization/
│   ├── service/      NormalizationService, ExtractionPromptBuilder, ConfidenceScorer
│   ├── repository/   StagedRecordRepository
│   ├── entity/       StagedRecord
│   └── dto/          ExtractionResult (internal contract)
│
├── validation/
│   ├── controller/   ValidationController
│   ├── service/      ValidationService, AliasGroupService
│   ├── repository/   ValidationItemRepository, ValidationActionRepository
│   ├── entity/       ValidationItem, ValidationAction
│   ├── dto/          ValidationDtos (QueueRow, ItemDetail, EditRequest, BulkRequest/Result,
│   │                 AliasGroupMapRequest, SummaryResponse)
│   └── event/        ValidationCompleted, MachineAliasCreated (co-owned with resolution)
│
├── search/
│   ├── controller/   SearchController
│   ├── service/      SearchService, QueryExpansionService, EmbeddingService
│   ├── repository/   RecordEmbeddingRepository
│   ├── entity/       RecordEmbedding
│   └── dto/          SearchDtos (OmniResponse, RecordHit with why[])
│
├── assistant/
│   ├── controller/   AssistantController
│   ├── service/      AssistantService, QueryUnderstandingService, AssistantToolExecutor,
│   │                 AnswerComposerService, CitationBuilder, NumericGuardrail
│   ├── repository/   AiConversationRepository, AiMessageRepository, AiCitationRepository
│   ├── entity/       AiConversation, AiMessage, AiCitation
│   └── dto/          AssistantDtos (QueryRequest, AnswerResponse w/ typed blocks,
│                     ConversationDtos, CitationDtos, FeedbackRequest)
│
├── analytics/
│   ├── controller/   AnalyticsController
│   ├── service/      AnalyticsService, KpiCalculator
│   └── dto/          AnalyticsDtos (KpiResponse, TrendResponse, ParetoResponse, TopMachinesResponse)
│
├── pattern/
│   ├── controller/   PatternController
│   ├── service/      PatternScanService, PatternReviewService,
│   │                 detector/ (RecurrenceDetector, TempFixRepeatDetector, CrossMachineDetector,
│   │                 PartConcentrationDetector, DowntimeHotspotDetector implementing PatternDetector)
│   ├── repository/   PatternRepository, PatternEvidenceRepository
│   ├── entity/       Pattern, PatternEvidence
│   ├── dto/          PatternDtos (ListRow, Detail, ReviewRequest, EvidenceRow)
│   └── event/        PatternDetected
│
├── whatsapp/
│   ├── controller/   WhatsAppWebhookController, WhatsAppAdminController
│   ├── service/      WhatsAppWebhookService, ConversationEngine, WhatsAppSendService
│   ├── repository/   WhatsAppContactRepository, WhatsAppConversationRepository, WhatsAppMessageRepository
│   ├── entity/       WhatsAppContact, WhatsAppConversation, WhatsAppMessage
│   ├── dto/          WebhookPayload (provider), ConversationDtos, ContactDtos
│   └── event/        WhatsAppRecordConfirmed
│
├── dashboard/
│   ├── controller/   DashboardController
│   ├── service/      DashboardService
│   └── dto/          DashboardResponse, InsightCard
│
├── report/            (V1-NICE)
│   ├── controller/   ReportController
│   ├── service/      ReportService, generators (CsvRecordsGenerator, MonthlyPdfGenerator)
│   ├── repository/   ReportRepository · entity/ Report · dto/ ReportDtos
│
├── notification/      (V1-NICE)
│   ├── controller/   NotificationController · service/ NotificationService
│   ├── repository/   NotificationRepository · entity/ Notification · dto/ NotificationDtos
│
├── audit/
│   ├── controller/   AuditLogController
│   ├── service/      AuditService (+ @Audited AOP aspect)
│   ├── repository/   AuditLogRepository · entity/ AuditLog · dto/ AuditDtos
│
├── jobs/
│   ├── controller/   JobController
│   ├── service/      JobService, JobRunner (executor bridge)
│   ├── repository/   JobRepository · entity/ Job · dto/ JobDtos
│
└── integration/                     outbound ports & adapters (no business logic)
    ├── llm/          LlmClient (port), AnthropicLlmClient (adapter), LlmRequest/Response
    ├── embedding/    EmbeddingClient (port), adapter impl
    ├── whatsappapi/  WhatsAppClient (port), MetaCloudApiClient (adapter), SignatureVerifier
    ├── storage/      StorageClient (port), S3StorageClient, LocalFsStorageClient
    └── transcription/ TranscriptionClient (port), adapter (optional)
```

## Per-package conventions

- **Controller**: HTTP only — request DTO validation, principal extraction, delegate, map to
  response DTO. No business logic, no repositories.
- **Service**: transactional business logic; plant-scope check first line; publishes events.
- **Repository**: Spring Data JPA + named native queries for analytics/vector operations.
- **DTO**: records; request DTOs carry Bean Validation; entities never cross the controller boundary.
- **Mapper**: MapStruct, one per aggregate; only where entity↔DTO mapping is non-trivial.
- **Entity**: JPA, extends `BaseEntity`; soft-deletables add `deleted_at` via `SoftDeletable`.
- **Exception**: modules throw `common.exception` types with module error codes (doc 17).
- **Configuration**: all in `config/` — modules stay annotation-light and testable.
- Module boundary rule: modules call each other **only through services** (no cross-module
  repository imports) — enforced with ArchUnit tests (doc 24).
```
src/main/resources
├── application.yml (+ application-dev / application-prod)
├── db/migration/ (Flyway V1__…, numbered per phase — see doc 26)
└── dictionaries/shorthand.yml, devanagari-terms.yml
```
