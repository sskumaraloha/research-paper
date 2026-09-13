# 13 — Spare Parts & Part Analysis

Answers "6205 bearing kahan kahan lagi hai?" deterministically, from `record_parts` joins —
no inventory management in V1 (LogSense observes consumption from history; it is not a stores
system).

## 1. Data model

```
parts (plant-scoped catalog: code "6205ZZ", name, uom)
record_parts (record_id, part_id, quantity)   ← created by normalization/validation/manual entry
```
Part identity is the **code**, matched case/format-insensitively ("6205zz", "6205 ZZ" →
6205ZZ). Unknown codes discovered during ingestion are flagged; approval may create the part
(`NEW_PART_SUSPECT` handling, doc 09 §2.7) — the catalog grows from real usage.

## 2. Deterministic analytics (PartUsageService)

`GET /api/v1/parts/{id}/usage?dateFrom&dateTo&plantId` returns (doc 03 §6.9):
- machines using the part, replacement count per machine, share % per machine and per line
- total replacements, first/last used, average interval days between uses
- timeline of usage events (each → record id → source)
- failure-mode association (top modes on records where the part appears)
- `computedBy: DETERMINISTIC`

Same service backs the assistant tool `part_usage` and the dashboard's part-concentration
insight — one numeric truth.

## 3. Catalog APIs

`GET/POST/PATCH /parts`, `GET /parts/{id}` — doc 03 §6.5–6.8. List rows carry SQL-computed
`machineCount, usageCount, lastUsedAt, trend` (trend = usage-rate bucket over trailing 12 m:
HIGH/MEDIUM/STABLE — thresholded, not predicted).

## 4. Machine-side view

`GET /machines/{id}/parts` — parts consumed on one machine with counts and last-used (drives
the machine detail panel).

## 5. Question routing

| Question | Path |
|---|---|
| "6205 bearing kahan kahan lagi hai?" | assistant intent PART_USAGE → part code regex → `part_usage` tool |
| "Which machines share parts with X?" | FUTURE (cross-part similarity) |
| "V-Belt A42 kitni baar laga?" | same tool, count emphasis |

## 6. V1 boundaries

No stock levels, reorder points, vendors, or costs (downtime cost is plant-level config, not
part cost). These are CMMS/stores concerns — FUTURE integrations, not V1 (doc 27).
