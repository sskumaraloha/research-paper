# API MASTER INDEX

Single quick reference. AUTH = minimum role (per-plant unless noted; hierarchy
VIEWER < TECHNICIAN < ENGINEER < PLANT_ADMIN < SUPER_ADMIN). ◇ = V1-NICE.
Full contracts: `docs/03-api-catalog.md`.

| METHOD | ENDPOINT | MODULE | AUTH | PURPOSE |
|---|---|---|---|---|
| POST | /api/v1/auth/login | Auth | Public | Login, issue tokens |
| POST | /api/v1/auth/refresh | Auth | Public (token) | Rotate refresh, new access |
| POST | /api/v1/auth/logout | Auth | Authenticated | Revoke refresh token |
| GET | /api/v1/auth/me | Auth | Authenticated | Current user + roles + permissions |
| POST | /api/v1/auth/change-password | Auth | Authenticated | Self password change |
| GET | /api/v1/users | User | PLANT_ADMIN | List users |
| POST | /api/v1/users | User | PLANT_ADMIN | Create user |
| GET | /api/v1/users/{id} | User | PLANT_ADMIN | User detail |
| PATCH | /api/v1/users/{id} | User | PLANT_ADMIN | Update / (de)activate |
| DELETE | /api/v1/users/{id} | User | SUPER_ADMIN | Deactivate (soft) |
| PUT | /api/v1/users/{id}/plant-roles | User | PLANT_ADMIN | Assign per-plant roles |
| GET | /api/v1/roles | User | Authenticated | Role catalog |
| GET | /api/v1/plants | Plant | VIEWER | List own plants |
| POST | /api/v1/plants | Plant | SUPER_ADMIN | Create plant |
| GET | /api/v1/plants/{id} | Plant | VIEWER | Plant detail |
| PATCH | /api/v1/plants/{id} | Plant | PLANT_ADMIN | Update plant |
| GET | /api/v1/plants/{id}/lines | Plant | VIEWER | List lines |
| POST | /api/v1/plants/{id}/lines | Plant | PLANT_ADMIN | Create line |
| PATCH | /api/v1/lines/{id} | Plant | PLANT_ADMIN | Update line |
| DELETE | /api/v1/lines/{id} | Plant | PLANT_ADMIN | Soft-delete line |
| GET | /api/v1/plants/{id}/settings | Plant | ENGINEER | Read plant settings |
| PUT | /api/v1/plants/{id}/settings | Plant | PLANT_ADMIN | Update settings/thresholds |
| GET | /api/v1/machines | Machine | VIEWER | Machine master list |
| POST | /api/v1/machines | Machine | ENGINEER | Create machine (+seed aliases) |
| GET | /api/v1/machines/{id} | Machine | VIEWER | Machine detail |
| PATCH | /api/v1/machines/{id} | Machine | ENGINEER | Update machine |
| DELETE | /api/v1/machines/{id} | Machine | PLANT_ADMIN | Soft-delete machine |
| GET | /api/v1/machines/{id}/history | Machine | VIEWER | Maintenance timeline |
| GET | /api/v1/machines/{id}/stats | Machine | VIEWER | MTTR/MTBF/Pareto (SQL) |
| GET | /api/v1/machines/{id}/parts | Machine | VIEWER | Parts consumed |
| GET | /api/v1/machines/{id}/patterns | Machine | VIEWER | Patterns on machine |
| GET | /api/v1/machine-aliases | Resolution | ENGINEER | List aliases |
| POST | /api/v1/machine-aliases | Resolution | ENGINEER | Map alias → machine |
| DELETE | /api/v1/machine-aliases/{id} | Resolution | ENGINEER | Remove alias |
| POST | /api/v1/machine-resolution/resolve | Resolution | Authenticated | Text → machine candidates |
| GET | /api/v1/failure-modes | Maintenance | VIEWER | Taxonomy list |
| POST | /api/v1/failure-modes | Maintenance | PLANT_ADMIN | Add mode |
| PATCH | /api/v1/failure-modes/{id} | Maintenance | PLANT_ADMIN | Edit mode/synonyms |
| DELETE | /api/v1/failure-modes/{id} | Maintenance | PLANT_ADMIN | Delete (merge) mode |
| GET | /api/v1/parts | Parts | VIEWER | Part catalog + usage aggregates |
| POST | /api/v1/parts | Parts | ENGINEER | Create part |
| GET | /api/v1/parts/{id} | Parts | VIEWER | Part detail |
| PATCH | /api/v1/parts/{id} | Parts | ENGINEER | Update part |
| GET | /api/v1/parts/{id}/usage | Parts | VIEWER | Usage analysis (machines/share/interval) |
| GET | /api/v1/maintenance-records | Maintenance | VIEWER | Global history (filters) |
| POST | /api/v1/maintenance-records | Maintenance | TECHNICIAN | Manual entry |
| GET | /api/v1/maintenance-records/{id} | Maintenance | VIEWER | Record detail |
| PATCH | /api/v1/maintenance-records/{id} | Maintenance | ENGINEER | Audited correction |
| DELETE | /api/v1/maintenance-records/{id} | Maintenance | PLANT_ADMIN | Soft delete (reason) |
| GET | /api/v1/maintenance-records/{id}/source | Maintenance | VIEWER | Raw provenance (View Source) |
| POST | /api/v1/imports | Ingestion | ENGINEER | Upload file → job |
| GET | /api/v1/imports | Ingestion | ENGINEER | List import jobs |
| GET | /api/v1/imports/{id} | Ingestion | ENGINEER | Job status/progress/counts |
| GET | /api/v1/imports/{id}/preview | Ingestion | ENGINEER | Parsed sample + suggested mapping |
| PUT | /api/v1/imports/{id}/mapping | Ingestion | ENGINEER | Confirm column mapping |
| POST | /api/v1/imports/{id}/process | Ingestion | ENGINEER | Start async pipeline (202) |
| POST | /api/v1/imports/{id}/cancel | Ingestion | ENGINEER | Cancel run |
| GET | /api/v1/imports/{id}/rows | Ingestion | ENGINEER | Row outcomes (skipped/failed/…) |
| POST | /api/v1/imports/{id}/retry | Ingestion | ENGINEER | Retry failed rows |
| GET | /api/v1/imports/{id}/file | Ingestion | ENGINEER | Download original (audited) |
| GET | /api/v1/validation/queue | Validation | ENGINEER | Open items |
| GET | /api/v1/validation/summary | Validation | ENGINEER | Counts + alias groups |
| GET | /api/v1/validation/items/{id} | Validation | ENGINEER | Item detail |
| POST | /api/v1/validation/items/{id}/approve | Validation | ENGINEER | Approve → record |
| PUT | /api/v1/validation/items/{id} | Validation | ENGINEER | Edit + approve |
| POST | /api/v1/validation/items/{id}/reject | Validation | ENGINEER | Reject (reason) |
| POST | /api/v1/validation/bulk-approve | Validation | ENGINEER | Bulk approve (partial success) |
| POST | /api/v1/validation/bulk-reject | Validation | ENGINEER | Bulk reject |
| POST | /api/v1/validation/alias-groups/map | Validation | ENGINEER | Map alias group (e.g. 37 records) |
| GET | /api/v1/validation/items/{id}/history | Validation | ENGINEER | Review trail |
| GET | /api/v1/search | Search | VIEWER | Omni search (grouped) |
| GET | /api/v1/search/records | Search | VIEWER | Hybrid record search + why[] |
| POST | /api/v1/assistant/query | AI | VIEWER | Ask question → grounded blocks |
| GET | /api/v1/assistant/conversations | AI | VIEWER (own) | List conversations |
| GET | /api/v1/assistant/conversations/{id} | AI | VIEWER (owner) | Conversation detail |
| DELETE | /api/v1/assistant/conversations/{id} | AI | VIEWER (owner) | Delete conversation |
| GET | /api/v1/assistant/messages/{id}/citations | AI | VIEWER | Citations of an answer |
| ◇ POST | /api/v1/assistant/messages/{id}/feedback | AI | VIEWER | Rate answer |
| GET | /api/v1/analytics/kpis | Analytics | VIEWER | MTTR/MTBF/downtime/breakdowns (+deltas) |
| GET | /api/v1/analytics/downtime-trend | Analytics | VIEWER | Trend points |
| GET | /api/v1/analytics/pareto | Analytics | VIEWER | Failure-mode Pareto |
| GET | /api/v1/analytics/top-machines | Analytics | VIEWER | Ranked machines |
| GET | /api/v1/analytics/parts-consumption | Analytics | VIEWER | Top parts |
| GET | /api/v1/analytics/lines | Analytics | VIEWER | Per-line rollup |
| GET | /api/v1/patterns | Pattern | VIEWER | List patterns |
| GET | /api/v1/patterns/{id} | Pattern | VIEWER | Pattern detail (fact+hypothesis) |
| GET | /api/v1/patterns/{id}/evidence | Pattern | VIEWER | Evidence records |
| POST | /api/v1/patterns/{id}/review | Pattern | ENGINEER | Acknowledge/Confirm/Dismiss (human) |
| POST | /api/v1/patterns/scan | Pattern | ENGINEER | Trigger scan (202) |
| GET | /api/v1/integrations/whatsapp/webhook | WhatsApp | Public (verify token) | Provider verification |
| POST | /api/v1/integrations/whatsapp/webhook | WhatsApp | Public (HMAC) | Inbound messages |
| GET | /api/v1/whatsapp/conversations | WhatsApp | ENGINEER | Monitor conversations |
| GET | /api/v1/whatsapp/conversations/{id} | WhatsApp | ENGINEER | Transcript + record link |
| POST | /api/v1/whatsapp/contacts | WhatsApp | PLANT_ADMIN | Register technician phone |
| DELETE | /api/v1/whatsapp/contacts/{id} | WhatsApp | PLANT_ADMIN | Unregister phone |
| GET | /api/v1/dashboard | Dashboard | VIEWER | Composite landing payload |
| GET | /api/v1/dashboard/insights | Dashboard | VIEWER | Insight feed (labelled) |
| ◇ POST | /api/v1/reports | Report | ENGINEER | Generate export (202) |
| ◇ GET | /api/v1/reports | Report | ENGINEER | List reports |
| ◇ GET | /api/v1/reports/{id}/download | Report | ENGINEER | Download export |
| GET | /api/v1/jobs/{id} | Jobs | Authenticated | Poll async job |
| ◇ GET | /api/v1/jobs | Jobs | ENGINEER | List jobs |
| GET | /api/v1/audit-logs | Audit | PLANT_ADMIN | Query audit trail |
| ◇ GET | /api/v1/notifications | Notification | Authenticated | Own notifications |
| ◇ POST | /api/v1/notifications/{id}/read | Notification | Authenticated | Mark read |
| GET | /actuator/health | Infra | Public (liveness) | Health check |

**Totals: 105 endpoints** (98 V1 core + 7 ◇ V1-NICE) across 19 business modules
(17 V1 core + 2 V1-NICE) plus shared plumbing (common/config/jobs/integration).
