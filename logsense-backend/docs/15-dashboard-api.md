# 15 — Dashboard APIs

The dashboard is a **composition layer**: it owns no numbers. Every figure comes from
`AnalyticsService` (SQL), every insight from `pattern`, every count from its owning module.
`computedBy: "DETERMINISTIC"` is stamped on all numeric sections — the LLM is not in this path
at all.

## 1. `GET /api/v1/dashboard?plantId&dateFrom&dateTo`

One call for the landing screen (VIEWER+):

```json
{
  "window": {"from":"2025-09-13","to":"2026-09-13","compare":"PREVIOUS_PERIOD"},
  "kpis": {                                        // AnalyticsService.KpiCalculator (SQL)
    "mttrHours":2.8,"mttrDeltaPct":-18.4,
    "mtbfHours":184,"mtbfDeltaPct":12.7,
    "downtimeHours":142.5,"downtimeDeltaPct":-14.2,
    "breakdowns":87,"breakdownsDeltaPct":-9.6,
    "recordsIndexed":1842,                          // count of ACTIVE maintenance_records
    "validationPending":43,                         // ValidationService count
    "computedBy":"DETERMINISTIC"},
  "downtimeTrend": {"granularity":"MONTH","points":[{"period":"2026-01","downtimeHours":24.5}],"computedBy":"DETERMINISTIC"},
  "topMachines":  {"metric":"DOWNTIME","rows":[{"machineId":"…","name":"Line 3 Conveyor Motor","downtimeHours":18.9,"breakdowns":12}],"computedBy":"DETERMINISTIC"},
  "failurePareto":[{"failureMode":"Bearing","count":21,"downtimeHours":41.2}],
  "insights": [                                     // top 3 patterns, labels preserved
    {"patternId":"…","type":"RECURRENCE","machine":"Line 3 Conveyor Motor",
     "fact":{"label":"CALCULATED","summary":"4 bearing replacements, avg interval 92 days","evidenceCount":4},
     "hypothesis":{"label":"HYPOTHESIS","confidence":"POSSIBLE","text":"Possible mounting/alignment issue"},
     "cta":{"kind":"MACHINE","id":"…"}}],
  "recentRecords":[ …5 latest record summaries… ]
}
```
Errors: 400/401, 404 unknown plant, 422 bad window. Sections computed in parallel; a failing
section degrades to `null` + `warnings[]` rather than failing the whole call.

## 2. `GET /api/v1/dashboard/insights?plantId&page&size`

Full insight feed (VIEWER+): all non-dismissed patterns rendered as cards with fact metrics,
labelled hypothesis, evidence counts and CTA targets. Backed by `GET /patterns` with dashboard
projection — no separate storage.

## 3. Who computes what (explicit)

| Dashboard element | Computed by | Never by |
|---|---|---|
| MTTR/MTBF/downtime/breakdowns + deltas | SQL (`KpiCalculator`) | LLM |
| Trend, Pareto, top machines, parts consumption | SQL (`AnalyticsService`) | LLM |
| Records indexed, validation pending | SQL counts | LLM |
| Insight *metrics* (intervals, counts, shares) | Pattern detectors (SQL/Java) | LLM |
| Insight *hypothesis sentence* | Template (+optional LLM wording polish, no numbers) | — |
| Plant health status per machine | Derived rule (breakdown density + open patterns) | LLM |

## 4. Related endpoints

Deep-dive equivalents live in `/analytics/*` (doc 03 §12) so the dashboard stays one
screen-shaped call while analysts can query precise windows/filters. Reports (V1-NICE)
export the same numbers via `POST /reports`.

## 5. Caching

KPI + trend sections cached 60 s per (plant, window) in-process (Caffeine), invalidated by
`MaintenanceRecordCreated/Updated` and `ValidationCompleted` events — dashboard stays snappy
without a Redis dependency in V1.
