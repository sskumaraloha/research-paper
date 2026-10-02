# API reference sheets (SVG)

Generated from [`../../openapi.json`](../../openapi.json) by
[`../../scripts/generate-api-diagrams.py`](../../scripts/generate-api-diagrams.py). All 88 operations are covered.

| Sheet | Contents |
|---|---|
| [00-overview.svg](00-overview.svg) | Totals, map of every endpoint, request conventions, `Page<T>`, `AuthResponse`, call lifecycles |
| [01-auth-users-plants.svg](01-auth-users-plants.svg) | auth (9), users (5), plants (4), config (2), platform admin (3) |
| [02-machines-records-parts-search.svg](02-machines-records-parts-search.svg) | machines (10), records (7), spare parts (2), search (3) |
| [03-imports-validation-entry-schedules.svg](03-imports-validation-entry-schedules.svg) | imports (4), validation (8), guided entry (5), WhatsApp webhook (1), schedules (5) |
| [04-dashboard-analytics-insights-assistant.svg](04-dashboard-analytics-insights-assistant.svg) | dashboard (1), analytics (6), insights (3), assistant (4), notifications (5), audit (1) |

Each endpoint card lists its parameters (path, query or header), request body fields and response fields, with types and
validation limits. `*` marks a required field. A schema used by several endpoints on a sheet, or nested in another
schema, is drawn once in that sheet's **Types** section.

Regenerate after the spec changes:

```bash
python3 frontend/scripts/generate-api-diagrams.py
```
