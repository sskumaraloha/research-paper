# 04 — End-to-End API Workflows

Every product workflow expressed as the exact API sequence that completes it.
All endpoints exist in `docs/03-api-catalog.md` (consistency audited).

---

## Workflow 1 — Login & landing

```
POST /api/v1/auth/login                      → tokens + plant roles
        ↓
GET  /api/v1/auth/me                         → permissions (UI menus)
        ↓
GET  /api/v1/dashboard?plantId=…             → KPIs, insights, pending validation
        ↓ (background, silent)
POST /api/v1/auth/refresh                    → before access-token expiry
```

## Workflow 2 — Upload historical maintenance data

```
POST /api/v1/imports                (multipart file + plantId)      → importJobId, status CREATED
        ↓ (system parses async)
GET  /api/v1/imports/{id}                                            → status PARSED, counts.detected=1842
GET  /api/v1/imports/{id}/preview                                    → suggested column mapping
PUT  /api/v1/imports/{id}/mapping                                    → mapping confirmed
        ↓
POST /api/v1/imports/{id}/process                                    → 202 jobId
        ↓ (poll)
GET  /api/v1/jobs/{jobId}   or   GET /api/v1/imports/{id}            → PROCESSING (stage, progressPct)
        ↓ pipeline: normalize → resolve machines → route
GET  /api/v1/imports/{id}                                            → COMPLETED
     counts: detected 1842 / usable 1790 / skipped 52 / needsReview 43
        ↓
GET  /api/v1/imports/{id}/rows?status=SKIPPED                        → skip reasons (audit trail)
        ↓
→ Workflow 3 (validation)
```

## Workflow 3 — Validation & alias mapping (the "37 records" moment)

```
GET  /api/v1/validation/summary?plantId=…
        → open=43, aliasGroups=[{groupKey:"conv motor-3", count:37,
                                 suggested: Line 3 Conveyor Motor @ 0.96}]
        ↓
POST /api/v1/validation/alias-groups/map
     {groupKey:"conv motor-3", machineId:…, createAlias:true}
        → 37 items approved, 37 records created, machine_alias learned
        ↓
GET  /api/v1/validation/queue                                        → 6 individual items left
GET  /api/v1/validation/items/{id}                                   → raw vs extracted, per-field confidence
POST /api/v1/validation/items/{id}/approve        (or PUT edit+approve, or POST reject)
        ↓
GET  /api/v1/validation/summary                                      → open=0
Side effects: MaintenanceRecordCreated → embeddings indexed, stats fresh, pattern scan queued
```

## Workflow 4 — Machine exploration

```
GET /api/v1/machines?plantId=…&q=conveyor&sort=recordCount,desc
        ↓
GET /api/v1/machines/{id}                        → metadata + aliases + status
GET /api/v1/machines/{id}/stats                  → MTTR/MTBF/downtime/Pareto  (CALCULATED)
GET /api/v1/machines/{id}/history?page=0&size=20 → timeline
GET /api/v1/machines/{id}/parts                  → parts consumed
GET /api/v1/machines/{id}/patterns               → recurrence card
        ↓ per timeline row ("View Source")
GET /api/v1/maintenance-records/{recordId}/source → raw text + file/sheet/row
```

## Workflow 5 — AI question with citations

```
POST /api/v1/assistant/query
     {"question":"Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?"}
        ↓ internally (doc 07):
        query understanding → resolve_machine → machine_stats (SQL)
        → search_records (hybrid) → pattern lookup → LLM composes → citations attached
        ↓
Response: SUMMARY(FACT) + KEY_FAILURES(FACT) + STATISTICS(CALCULATED, coverage 12/12)
          + PATTERN(CALCULATED ~92 days) + HYPOTHESIS(POSSIBLE, disclaimer) + citations
        ↓ user clicks a citation
GET /api/v1/assistant/messages/{id}/citations
GET /api/v1/maintenance-records/{recordId}/source        → original raw entry drawer
        ↓ follow-up in same conversation
POST /api/v1/assistant/query {conversationId, "Total downtime bearing failures Line 3 last 2 years"}
        → STATISTICS block only: 18.7 hrs · 7 records · 7/7 valid downtime · CALCULATED
```

## Workflow 6 — Deterministic statistics for dashboard

```
GET /api/v1/analytics/kpis?plantId&dateFrom&dateTo&compareToPrevious=true
GET /api/v1/analytics/downtime-trend?granularity=MONTH
GET /api/v1/analytics/top-machines?metric=DOWNTIME&limit=5
GET /api/v1/analytics/pareto
(all SQL-computed; the assistant reuses the same AnalyticsService via tools — one numeric truth)
```

## Workflow 7 — Spare-part intelligence ("6205 bearing kahan kahan lagi hai?")

```
Path A (UI):        GET /api/v1/parts?q=6205 → GET /api/v1/parts/{id}/usage
Path B (assistant): POST /assistant/query → tool part_usage → same PartUsageService
        → machines[3], byLine[Line 3: 60%], avgIntervalDays, timeline, citations
```

## Workflow 8 — WhatsApp maintenance entry

```
Technician → WhatsApp: "Line 3 ka conveyor motor band tha, bearing change kiya,
                        alignment check kiya, ab chal raha hai."
        ↓
POST /api/v1/integrations/whatsapp/webhook        (signature verified, 200 fast-ack)
        ↓ async ConversationEngine
identify contact (phone→user→plant) → NormalizationService extract
        → missing field: downtime → outbound: "Machine kitni der band thi?"
        ↓ technician: "2 ghante" → webhook again
        → AWAITING_CONFIRMATION: structured echo + Confirm button (interactive message)
        ↓ technician confirms → webhook (button reply)
StagedRecord(source=WHATSAPP, confidence high) → auto-approve → MaintenanceRecord #…
        → outbound: "Record #2048 created ✅"
        → MaintenanceRecordCreated → recurrence detector → PatternDetected (5th bearing)
        → notification to engineers: "Requires engineering review"
        ↓ manager verifies in UI
GET /api/v1/whatsapp/conversations/{id}            → transcript + record link
GET /api/v1/machines/{id}/history                  → new record, source WHATSAPP
```

## Workflow 9 — Pattern review (hypothesis → human decision)

```
(nightly / post-import)  POST /api/v1/patterns/scan  (or scheduler)
        ↓
GET /api/v1/patterns?status=DETECTED
GET /api/v1/patterns/{id}            → fact metrics + labelled hypothesis + evidence
GET /api/v1/patterns/{id}/evidence   → each evidence → record → source
        ↓ engineer inspects machine, decides
POST /api/v1/patterns/{id}/review {"action":"CONFIRM","note":"base-frame resonance found"}
     — CONFIRMED is reachable only through this human call
```

## Workflow 10 — Manual record entry (UI)

```
POST /api/v1/machine-resolution/resolve   (autocomplete while typing machine)
POST /api/v1/maintenance-records          → 201 (raw_record kept for provenance)
GET  /api/v1/machines/{id}/history        → appears immediately
```

## Workflow 11 — User administration

```
POST /api/v1/users                        → create engineer/technician
PUT  /api/v1/users/{id}/plant-roles       → assign plant role
POST /api/v1/whatsapp/contacts            → bind technician phone
PATCH /api/v1/users/{id} {active:false}   → offboard (sessions revoked)
GET  /api/v1/audit-logs?actorId=…         → verify trail
```

## Workflow 12 — Global search

```
GET /api/v1/search?q=brng noise conveyor&plantId=…
        → expansion [bearing, brng, brg, बेयरिंग] → grouped machines/parts/records/patterns
GET /api/v1/search/records?q=bearing gaya&machineId=…    → paginated with why[]
→ record click → GET /maintenance-records/{id}/source
```

## Workflow 13 — Failure-mode taxonomy upkeep

```
GET /api/v1/failure-modes → POST (add "Cavitation") → PATCH (synonyms)
DELETE /api/v1/failure-modes/{id} {mergeIntoId} → records re-pointed, audited
```

## Coverage check

Every frontend/demo capability maps to a workflow above: dashboard (1,6), upload (2),
validation + alias (3), machine detail + source drawer (4), AI chat + citations (5),
deterministic stats (6), parts (7), WhatsApp (8), patterns (9), manual entry (10),
admin (11), search (12), taxonomy (13). No orphan endpoints; no missing APIs.
