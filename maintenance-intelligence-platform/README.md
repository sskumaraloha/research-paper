# Maintenance Intelligence Platform

A Spring Boot backend for plant maintenance intelligence: maintenance records flow in
through an **intelligent import pipeline** (CSV/XLSX parsing → normalisation → machine
resolution → failure-mode extraction → confidence scoring → routing), get validated by
humans where the pipeline is unsure, and feed **analytics, KPIs, pattern-detection
insights, search and a data-backed assistant**.

## Modules

```
user / security   JWT auth (access + rotating hashed refresh tokens), roles ADMIN/ENGINEER/VIEWER
plant             Plants, production lines, per-plant pipeline thresholds, plant-scoped access
dictionary        Failure modes (keyword-driven) and search synonyms
machine           Machines, normalised aliases, tiered machine resolver, alias suggestions
part              Spare parts with usage stats and replacement intervals
record            Maintenance records, source documents, timelines, filters
importjob         Import jobs, per-step progress, staged rows, the pipeline itself
validation        Human validation queue (approve / edit-approve / reject), alias mapping
analytics         Machine stats, pareto, trends, line share, plant KPIs
insight           5 pattern detectors with evidence records
search            Synonym-expanded global/record/machine search
assistant         Intent-routed Q&A over the plant's own data
entry             Conversational record entry agent
notification      Per-user notifications and badge counts
```

## Key design decisions

- **Plant scoping everywhere**: non-admin users only reach plants assigned to them;
  an inaccessible plant, machine or record is a 404, never a 403 (no id probing).
- **Import pipeline is atomic**: a crash rolls the whole run back; the job is marked
  FAILED and can be re-run. Rows route by the plant's thresholds: auto-import at/above
  `autoApproveThreshold`, outright rejection below `lowConfidenceThreshold` (only when
  the machine is also unresolved), the validation queue in between.
- **The system learns**: approving a row with a manually-supplied machine, or mapping
  an alias suggestion, stores a machine alias — the same name auto-resolves in every
  later import, and mapping re-resolves the pending queue.
- **Deterministic intelligence**: machine resolution (exact code/name → alias → synonym
  → fuzzy Levenshtein), failure-mode extraction (dictionary keywords + synonyms) and
  intent routing are rule-based and fully testable; no external AI service is called.
- **Refresh-token hygiene**: opaque 256-bit tokens stored as SHA-256 hashes, rotated on
  every refresh, revoked on logout.

## Running

```bash
# Prerequisites: JDK 17+, Maven 3.8+, MySQL 8 for the dev profile

mvn test              # full suite on in-memory H2, no MySQL needed
mvn spring-boot:run   # dev profile: http://localhost:8080, MySQL mip_db auto-created

# Production
mvn package
export DB_URL='jdbc:mysql://<host>:3306/mip_db?useSSL=true&serverTimezone=UTC'
export DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=<base64 256-bit key>
java -jar target/maintenance-intelligence-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Dev seed users (dev profile only): `admin@mip.local`/`Admin@123` (ADMIN),
`demo@mip.local`/`Demo@123` (ENGINEER, used by `POST /api/auth/demo-login`),
`viewer@mip.local`/`Viewer@123` (VIEWER). Swagger UI (dev only):
`http://localhost:8080/swagger-ui.html`.

## API surface (~60 endpoints)

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/login`, `/demo-login`, `/refresh`, `/logout`, `GET /api/auth/me` |
| Users | `GET /api/users` (admin), `GET /api/users/roles` |
| Plants | `GET /api/plants`, `/{id}/settings`, `/{id}/lines` |
| Config | `GET /api/config/failure-modes`, `/dictionary` |
| Machines | `GET /api/machines`, `/{id}`, `/{id}/timeline`, `/{id}/stats`, `/{id}/insights`, `POST /{id}/insights/recompute`, `GET/POST /{id}/aliases` |
| Records | `GET/POST /api/records`, `GET /{id}`, `POST /{id}/reject`, `GET /filter-options`, `/source-documents` |
| Imports | `POST /api/imports/upload`, `GET /latest`, `/{id}`, `POST /{id}/rerun` |
| Validation | `GET /api/validation/queue`, `/pending-count`, `POST /{id}/approve`, `/{id}/edit-approve`, `/{id}/reject`, alias suggestions `GET /alias-suggestions`, `POST /alias-suggestions/{id}/map` |
| Dashboard | `GET /api/dashboard/kpis` |
| Analytics | `GET /api/analytics/pareto`, `/top-downtime-machines`, `/failure-mode-stats`, `/line-downtime-share`, `/part-replacement-intervals`, `/downtime-trend` |
| Insights | `GET /api/insights`, `/count`, `POST /recompute` |
| Parts | `GET /api/parts`, `/{id}` |
| Search | `GET /api/search`, `/records`, `/machines` |
| Assistant | `POST /api/assistant/ask`, `GET /api/assistant/suggestions` |
| Entry agent | `POST /api/entry/conversations`, `GET /{id}`, `POST /{id}/messages`, `/{id}/confirm`, `/{id}/request-edit` |
| Notifications | `GET /api/notifications`, `/unread-count`, `/badge-counts`, `POST /{id}/read`, `/read-all` |

Mutating endpoints require `ADMIN` or `ENGINEER`; `VIEWER` is read-only; user listing is
admin-only. All plant-scoped reads verify plant membership.

## Out of scope (blueprint items marked optional / needs-confirmation)

- `Organisation`, `AssistantConversation`/`AssistantMessage` entities (optional)
- `WhatsAppWebhookController` and inbound webhooks (needs confirmation; the entry agent
  is channel-agnostic and ready to be fronted by one)
- User CRUD (`createUser`/`updateUser`/`deactivateUser`), `updatePlantSettings`,
  `createMachine`/`updateMachine`, alias-suggestion `dismiss` (starred as needs
  confirmation in the blueprint)

## Testing

`mvn test` runs 23 integration tests (H2, real Spring context, real security filters):
auth/token lifecycle, plant scoping and role enforcement, the import pipeline
end-to-end (routing, validation queue, alias learning, duplicate rejection), record
lifecycle and validation rules, analytics/KPI math, all plant-level detectors,
assistant intent routing with real data, synonym search, and multi-turn entry-agent
conversations.
