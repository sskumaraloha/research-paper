# 06 — Entity Relationships

Cardinality legend: `1—N` one-to-many, `1—1`, `N—M` via join table.

## Core hierarchy

```
Plant 1—N Line 1—N Machine 1—N MaintenanceRecord
                                   ├── N—1 FailureMode      (nullable for PM)
                                   ├── N—M Part             (via RecordPart, with quantity)
                                   └── downtime_hours       (column, not entity)
Plant 1—1 PlantSettings
Plant 1—N FailureMode              (taxonomy is per plant)
Plant 1—N Part
```

## Identity

```
User N—M Plant  (via UserPlantRole, role attribute; SUPER_ADMIN flag is global on User)
User 1—N RefreshToken
User 1—N WhatsAppContact  (usually 1, unique phone)
```

## Machine resolution

```
Machine 1—N MachineAlias
   "Conv Motor-3", "CONV MTR 3", "L3 Conv Motor", "Line3 Conveyor Motor", "L3 conveyor"
   → all rows point to machine CONV-L3-MTR-01; normalized_alias is unique per plant.
MachineAlias.source ∈ {SEED, VALIDATION, MANUAL}  → provenance of the learning
```

## Ingestion → record provenance chain (the traceability backbone)

```
ImportJob 1—1 ImportFile
ImportJob 1—N RawRecord ──1—1── StagedRecord ──1—0..1── ValidationItem 1—N ValidationAction
                                     │
                                     └─(on approval)─→ MaintenanceRecord.raw_record_id  (NOT NULL)
```

Guarantees:
- Every `MaintenanceRecord` has exactly one immutable `RawRecord` ancestor
  (file/sheet/row, WhatsApp conversation, or manual entry).
- Nothing in normalization ever mutates `RawRecord` (Rule 5).
- A rejected staged record leaves the raw record intact with status REJECTED — nothing silently disappears.

## Patterns

```
MaintenanceRecord N—M Pattern   (via PatternEvidence)

Pattern
 ├── fact (jsonb metrics)        label CALCULATED — derived only from evidence records
 ├── hypothesis_text             label HYPOTHESIS — never numeric, never auto-CONFIRMED
 └── PatternEvidence 1—N ──→ MaintenanceRecord ──→ RawRecord   (evidence is clickable to source)
```

## AI traceability (Rule 2)

```
User 1—N AiConversation 1—N AiMessage 1—N AiCitation
                                             ├── N—1 MaintenanceRecord (nullable)
                                             └── N—1 RawRecord         (nullable, ≥1 of the two set)

AiAnswer block  →  Citation  →  MaintenanceRecord  →  RawRecord  →  ImportFile row / WhatsApp msg
```
Every FACT block in an answer carries ≥1 citation; STATISTICS blocks carry the tool name +
the record-set coverage they were computed over. A number with no deterministic tool origin
cannot appear in a response (enforced in AnswerComposer — doc 07).

## WhatsApp

```
WhatsAppContact 1—N WhatsAppConversation 1—N WhatsAppMessage (append-only)
WhatsAppConversation 0..1—1 StagedRecord ─→ MaintenanceRecord
   (so a record born on WhatsApp traces to the full conversation transcript)
```

## Async & audit

```
Job          — standalone; result_ref points at importJobId / patternScan / reportId
AuditLog     — polymorphic (entity_type, entity_id); references actor User
Notification — N—1 User
Report       — N—1 Plant
```

## Referential integrity rules

| Relationship | On delete |
|---|---|
| Plant → Line → Machine | soft delete only; hard delete forbidden once records exist |
| Machine → MaintenanceRecord | RESTRICT (machine soft-deleted, records retained & queryable) |
| FailureMode → MaintenanceRecord | RESTRICT; taxonomy delete requires merge (API 6.4) |
| RawRecord → MaintenanceRecord | RESTRICT — provenance can never be orphaned |
| MaintenanceRecord → PatternEvidence / AiCitation / RecordPart | soft-deleted record keeps rows; queries exclude via join filter |
| User → anything | never deleted; deactivated |

## ER diagram (compact)

```
users ──< user_plant_roles >── plants ──< lines ──< machines ──< maintenance_records >── failure_modes
  │                              │                     │               │  │
  │                              ├──< failure_modes    └──< machine_   │  └──< record_parts >── parts
  ├──< refresh_tokens            ├──< parts                 aliases    │
  ├──< whatsapp_contacts ──< whatsapp_conversations ──< whatsapp_msgs  ├──── raw_records >── import_jobs ── import_files
  ├──< ai_conversations ──< ai_messages ──< ai_citations ─────────────┤          │
  └──< audit_logs (actor)                                              │      staged_records ── validation_items ──< validation_actions
                                          pattern_evidence >───────────┘
                                               │
                                            patterns          record_embeddings (1—1 maintenance_records)
```
