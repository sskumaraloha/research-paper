# 12 — Pattern Detection

Deterministic detectors over approved maintenance records. The **metrics are FACT/CALCULATED;
the explanation is HYPOTHESIS** — and only a human can confirm anything (Rule 3).

## 1. Detector catalog (V1)

| type | Definition (deterministic) | Example output |
|---|---|---|
| `RECURRENCE` | ≥3 breakdowns, same machine + failure mode, interval CV ≤ 0.35 within 24 m | "4 bearing replacements on Line 3 Conveyor Motor, avg interval 92 days" + hypothesis "possible mounting/alignment issue" when alignment checks co-occur |
| `TEMP_FIX_REPEAT` | action classified temporary (clean/tighten/reset/re-tension) followed ≤ 21 d by breakdown of same mode on same machine, ≥2 cycles | "Sensor cleaned twice; stoppage recurred within 14 days each time until replacement" |
| `CROSS_MACHINE` | same failure mode on ≥3 machines of same type/line within 90 d | "VFD trips on 3 Line-2 machines in 6 weeks" |
| `PART_CONCENTRATION` | one part ≥ N uses with ≥60 % share on one line/machine group | "6205ZZ: 10 replacements, 60 % on Line 3" |
| `DOWNTIME_HOTSPOT` | line/machine downtime share ≥ threshold of plant total in window | "Line 3 carries 43 % of recorded downtime" |

Thresholds are constants per detector with per-plant overrides in `plant_settings`
(kept minimal: window months, min events).

## 2. When detection runs

```
Nightly scheduled scan per plant           (cron, jobs.type=PATTERN_SCAN)
After ImportCompleted                      (event → scan queued)
After MaintenanceRecordCreated             (targeted: detectors scoped to that machine —
                                            this powers "5th bearing replacement" firing
                                            seconds after the WhatsApp record lands)
Manual: POST /patterns/scan                (202 + jobId)
```

## 3. Lifecycle & the FACT/HYPOTHESIS boundary

```
DETECTED ── engineer ACKNOWLEDGE ──→ UNDER_REVIEW ──→ CONFIRMED (human only, note required)
    │                                                   └──→ DISMISSED (note required)
    └── re-evaluation on new evidence: metrics refresh, status preserved
```
- `fingerprint` (type + machineId/scope + failure mode) deduplicates: a re-scan **updates**
  the existing pattern's fact metrics and evidence instead of duplicating; DISMISSED
  fingerprints are suppressed from re-detection (a materially changed metric set — e.g. two
  new events — reopens as DETECTED with a note).
- DB trigger forbids `CONFIRMED` with `reviewed_by IS NULL` (doc 05) — the system cannot
  confirm its own hypothesis even by bug.
- Response shape always separates `fact{label:CALCULATED, metrics, evidenceCount}` from
  `hypothesis{label:HYPOTHESIS, confidence:POSSIBLE|LIKELY, disclaimer}` — "Root cause
  confirmed" text is unrepresentable in the wire format.

## 4. Evidence

Every pattern links its records via `pattern_evidence` (UNIQUE pattern+record); each evidence
row → maintenance record → raw record, so every insight is click-through traceable
(dashboard card → View evidence → source drawer). Evidence count is shown wherever the
pattern is shown; a pattern with < min evidence is never surfaced.

## 5. Hypothesis text generation

Hypothesis sentences come from a **template library per detector** (deterministic), optionally
polished by the LLM for language only — the LLM receives the computed metrics and may not add
numbers or new causes (post-validated same as assistant answers, doc 07 §2.5). V1 templates:
- RECURRENCE + alignment co-occurrence → "Repeated {mode} replacement despite alignment checks
  may indicate a mounting/alignment issue."
- TEMP_FIX_REPEAT → "Temporary fix ({action}) does not hold; component replacement or deeper
  inspection may be needed."
- CROSS_MACHINE → "Shared cause possible (supply quality, environment, batch of parts)."

## 6. APIs (full contracts in doc 03 §13)

`GET /patterns` (filters type/status/machine) · `GET /patterns/{id}` · `GET /patterns/{id}/evidence`
· `POST /patterns/{id}/review` (ACKNOWLEDGE/CONFIRM/DISMISS + note) · `POST /patterns/scan`.
Machine page consumes `GET /machines/{id}/patterns`; dashboard consumes `GET /dashboard/insights`.

## 7. Non-goals in V1

No ML anomaly detection, no prediction ("will fail on date X"), no sensor fusion — the PDF's
value is *recurrence made visible from history*, and deterministic detectors deliver exactly
that with full explainability.
