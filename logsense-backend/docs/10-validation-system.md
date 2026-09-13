# 10 — Validation System

Human review is a product feature, not an afterthought: low-confidence extractions are never
silently trusted (Rule 4).

## 1. What lands in the queue

A `staged_record` becomes a `validation_item` when any of:
| reason | trigger |
|---|---|
| `LOW_MACHINE_CONFIDENCE` | resolver best candidate < plant auto-approve threshold, or no candidate |
| `LOW_FIELD_CONFIDENCE` | any critical field (date, failure mode for breakdowns, action) below threshold |
| `MISSING_FIELDS` | unparseable/absent critical fields (e.g., `CONV TRIP L3 VFD` — no action/downtime) |
| `DUPLICATE_SUSPECT` | similar existing record (machine+date+text similarity ≥ 0.8) |

WhatsApp records normally bypass the queue (technician confirmed in-chat = human validation),
except DUPLICATE_SUSPECT which always queues.

## 2. Item anatomy (split-screen contract)

Left = immutable truth, right = editable extraction:
```
raw:        text + source (file → sheet → row | conversation | manual)
extracted:  date, machine{text,candidates[],chosen,confidence}, failureMode, action,
            parts[], downtimeHours, technician, kind
confidence: per field + overall
reason, group_key (normalized unresolved machine text), duplicate suspects[]
```

## 3. Actions & state machine

```
validation_items.status: OPEN → RESOLVED        (one-way; audited)
actions (validation_actions, append-only):
  APPROVE        accept extraction as-is           → record created
  EDIT_APPROVE   corrected fields + approve        → record created; before/after stored
  REJECT         mandatory reason                  → staged REJECTED; raw retained
  BULK_MAP       alias-group mapping               → N records created + alias learned
```
Concurrency: resolving an already-resolved item → 409 `ALREADY_RESOLVED` returning the
winning action (two engineers can't double-create records; optimistic status check in one
UPDATE … WHERE status='OPEN').

## 4. APIs (defined fully in doc 03 §9)

| API | Use |
|---|---|
| `GET /validation/queue` | filterable open items |
| `GET /validation/summary` | counts by reason + **alias groups** |
| `GET /validation/items/{id}` | full detail incl. machine candidates |
| `POST /validation/items/{id}/approve` | accept |
| `PUT /validation/items/{id}` | edit + approve |
| `POST /validation/items/{id}/reject` | reject with reason |
| `POST /validation/bulk-approve` / `bulk-reject` | up to 500 ids, partial-success response |
| `POST /validation/alias-groups/map` | the "37 records → Line 3 Conveyor Motor" action |
| `GET /validation/items/{id}/history` | review trail |

## 5. Alias-group mapping (flagship flow)

```
summary → aliasGroups: [{groupKey:"conv motor-3", count:37,
                         suggested:{machine:"Line 3 Conveyor Motor", confidence:0.96}}]
POST /validation/alias-groups/map {groupKey, machineId, createAlias:true}
  per item (transactional batch):
    staged.machineId = machineId, resolution_method = HUMAN
    → maintenance record created → item RESOLVED, action BULK_MAP
  once: machine_aliases += {alias:"Conv Motor-3", source:VALIDATION}
  emits MachineAliasCreated → any other OPEN items with same group_key across other
  imports are re-resolved automatically
response: {mappedItems:37, createdRecords:37, aliasId}
```
Suggestion source: resolver's best fuzzy/semantic candidate aggregated over the group
(majority + mean confidence). If confidence < alias_suggest_threshold, no suggestion is shown —
the engineer picks the machine manually.

## 6. Learning loop

Every EDIT_APPROVE that changes the machine, and every BULK_MAP, feeds `machine_aliases`
(source VALIDATION). Failure-mode corrections append to `failure_modes.synonyms` candidates
(admin-approved). The LLM itself is never fine-tuned by V1 corrections — learning lives in
deterministic dictionaries, so it is inspectable and reversible (doc 07 §8).

## 7. Review history & audit

`validation_actions` is the item-level trail (who, when, before/after, note); global
`audit_logs` records the same events with trace IDs. Rejections never delete raw data —
`GET /imports/{id}/rows?status=…` still shows them with their fate.

## 8. Queue hygiene

- Ordering default: alias groups first (highest leverage), then by overall_confidence asc.
- SLA metric: `validation_open_items` gauge + oldest-item age (doc 19); dashboard shows the
  pending count (doc 15).
- No auto-expiry: items stay OPEN until a human acts (data quality beats queue-zero vanity).
