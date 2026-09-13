# 18 — Domain Events

**Transport decision: in-process Spring application events** (`@TransactionalEventListener`,
AFTER_COMMIT) + the durable `jobs` table for anything that must survive a restart.
**Kafka: V1 NOT REQUIRED** — one deployable, no cross-service consumers, volumes in the
thousands/day. Event classes are plain records with stable payloads, so a broker publisher can
be added behind the same interfaces later (FUTURE).

## Event catalog

| Event | V1? | Producer | Consumers | Payload | Why it exists |
|---|---|---|---|---|---|
| `MaintenanceRecordCreated` | **V1 REQUIRED** | MaintenanceRecordService (all three birth paths) | EmbeddingService (index), MachineStatsService (cache evict), PatternScanService (targeted machine check → "5th bearing replacement" moment), DashboardService (cache evict) | recordId, machineId, plantId, failureModeId, kind, source | decouples the write path from indexing/analytics side-effects |
| `MaintenanceRecordUpdated` | **V1 REQUIRED** | MaintenanceRecordService (PATCH/soft delete) | EmbeddingService (re-embed/remove), caches, PatternScanService (re-evaluate affected patterns) | recordId, plantId, changedFields | corrections must propagate everywhere numbers are derived |
| `ImportCompleted` | **V1 REQUIRED** | ImportPipelineOrchestrator | PatternScanService (plant scan), NotificationService, AuditService | importJobId, plantId, counts | triggers scan + user feedback exactly once, after commit |
| `ValidationCompleted` | **V1 REQUIRED** | ValidationService (per item & bulk summary) | NotificationService, dashboard cache | itemIds, action, actorId, plantId | queue metrics + activity feed without coupling validation→dashboard |
| `MachineAliasCreated` | **V1 REQUIRED** | AliasService (manual, validation map) | Ingestion re-resolver (re-tries OPEN items sharing the normalized alias across imports) | aliasId, machineId, normalizedAlias, plantId | the learning loop: one mapping fixes every pending occurrence |
| `PatternDetected` | **V1 REQUIRED** | PatternScanService (new or materially updated pattern) | NotificationService ("requires engineering review"), dashboard insights cache | patternId, type, machineId?, plantId, factSummary | surfaces insights without polling |
| `WhatsAppRecordConfirmed` | **V1 REQUIRED** | ConversationEngine | (thin) emits `MaintenanceRecordCreated` downstream; WhatsAppSendService (ack message); AuditService | conversationId, recordId, contactId | keeps chat ack + record creation transactionally ordered |
| `UserLoggedIn` | V1 (audit-only) | AuthService | AuditService | userId, ip, ua | security trail |
| `ImportFileUploaded` | FUTURE | — | AV scanning pipeline | — | only when AV service exists |
| `RecordEmbedded` | FUTURE | — | external analytics | — | no V1 consumer |
| Broker-published integration events (`logsense.events.*`) | FUTURE | outbox publisher | external subscribers / extracted services | same payloads | only when a second deployable exists |

## Delivery semantics

- Listeners run AFTER_COMMIT; heavy consumers (embedding, pattern scan) immediately enqueue a
  `jobs` row / async task rather than doing work in the listener thread → restart-safe
  (at-least-once via job table; all consumers idempotent by natural keys: embeddings upsert,
  pattern fingerprint, notification dedupe key).
- Ordering: consumers never depend on cross-event ordering; each re-reads current DB state.
- Failure isolation: a throwing listener is logged + metered, never rolls back the producer
  (events fire post-commit by design).

## Non-events (deliberate)

Plant/line/machine CRUD, settings changes, user admin → **audit log only**; no consumer needs
a reactive hook, so no event class exists (Rule 9: no ceremony without a consumer).
