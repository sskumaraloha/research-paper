# 11 — Machine Resolution

One service (`MachineResolverService`) used identically by ingestion, validation suggestions,
WhatsApp, search, and the assistant — so "Conv Motor-3", "CONV MTR 3", "L3 Conv Motor",
"Line3 Conveyor Motor", "L3 conveyor", "कन्वेयर मोटर लाइन 3" all land on `CONV-L3-MTR-01`.

## 1. Normalization (deterministic, shared with search)

```
input  → lowercase → trim/squash whitespace → strip punctuation (keep #, digits)
       → expand shorthand dictionary: mtr→motor, conv→conveyor, m/c|mc→machine,
         brg|brng→bearing, comp→compressor, l3|line3→line 3 …
       → Devanagari term map: कन्वेयर/कन्व्हेयर→conveyor, मोटर→motor, मशीन→machine …
       → canonical token sequence  ("conv motor-3" → "conveyor motor 3")
```
Dictionary is code-versioned (yaml resource) + per-plant additions via settings — deterministic
and testable, deliberately not an LLM call.

## 2. Matching cascade (first confident hit wins; all candidates returned)

| # | Method | Match | Confidence basis |
|---|---|---|---|
| 1 | `EXACT` | machine code or full name (normalized) equality | 1.00 |
| 2 | `ALIAS` | `machine_aliases.normalized_alias` equality (unique per plant) | 0.95–0.99 (source-weighted: MANUAL/VALIDATION > SEED) |
| 3 | `FUZZY` | pg_trgm similarity vs names+aliases, token-set overlap, line-token agreement ("l3" must not match Line 2 machines — line token is a hard filter when present) | scaled 0.55–0.90 |
| 4 | `SEMANTIC` | embedding similarity of normalized text vs machine name/alias embeddings | scaled 0.50–0.85 |

Output: `{candidates:[{machineId, confidence, method}], resolved: top ≥ context threshold}`.
Thresholds: pipeline auto-accept = `plant.auto_approve_threshold` (default 0.90);
assistant accepts ≥ 0.75 but states the assumption ("assuming Line 3 Conveyor Motor");
search uses candidates only as boosts, never as hard filters unless resolved.

## 3. Alias storage & learning

- `machine_aliases(alias, normalized_alias UNIQUE per plant, source SEED|VALIDATION|MANUAL)`.
- SEED aliases created with the machine (API 4.2) or demo data.
- VALIDATION aliases created by `alias-groups/map` and machine-changing `EDIT_APPROVE`
  (doc 10 §6) — the resolver literally learns from corrections.
- MANUAL via `POST /machine-aliases` (doc 03 §5.2).
- Conflict rule: one normalized alias maps to exactly one machine per plant; remapping
  requires deleting the old alias (audited) — prevents silent flip-flopping of history.
- Aliases never rewrite existing records; they affect only future resolution. Re-pointing
  history is an explicit, audited record PATCH.

## 4. Manual override

Anywhere a resolution is shown, a human choice wins and is recorded as
`resolution_method = HUMAN` (staged record) — overrides feed learning as above; the resolver's
wrong suggestion is kept in the validation action's `before` for calibration analysis.

## 5. Validation of the resolver itself

- Golden test set: alias variants from the PDF + Hinglish/Devanagari forms (doc 24) must
  resolve with expected method + minimum confidence (CI-gated).
- Metric: auto-resolution rate and post-hoc correction rate per import (doc 19) — a rising
  correction rate signals dictionary/threshold drift.

## 6. API surface

`POST /machine-resolution/resolve` (debug/autocomplete), `GET/POST/DELETE /machine-aliases`
— doc 03 §5. Internal callers use the service directly (same code path, no HTTP loopback).
