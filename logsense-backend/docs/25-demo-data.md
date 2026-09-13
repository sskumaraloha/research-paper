# 25 — Seed / Demo Data

Purpose: a deterministic dataset for local dev, tests (doc 24 fixtures) and client demos —
mirroring the PDF examples and the existing frontend prototype (`logsense-demo/js/mock-data.js`
already encodes an internally consistent version of this dataset; the seed must reproduce its
headline numbers). Loaded via a Flyway "seed" profile migration or a `DemoDataLoader` runner
(dev/demo profiles only — never prod).

## 1. Structure

```
1 plant: Demo Pune Manufacturing Plant (PUN1, Asia/Kolkata, downtime cost ₹1,25,000/line-hr,
         auto-approve threshold 0.90)
5 lines: Line 1–4 (PRODUCTION), Utilities (UTILITY)
22 machines across lines (blister/press/conveyor/gearbox/blower/palletizer/packing/
   compressors/pumps/cooling tower/boiler/air dryer)
13 failure modes: Bearing, VFD, Sensor, Belt, Coupling, Alignment, Overheating,
   Electrical trip, Leakage, Lubrication, Preventive, Inspection, Other
7 parts: 6205ZZ, 6207 Bearing, Proximity Sensor, V-Belt A42, Contactor, Gland Packing,
   Coupling Spider
5 technicians (names only): Sunil, Amit, Rahul, Prakash, Vijay (+ Ramesh as WhatsApp user)
6 users: super admin, plant admin (Rajesh Kumar), engineer, 2 technicians, viewer
~230 maintenance records (16 months) + 1 import job with raw rows backing every record
```

## 2. Anchor machine: Line 3 Conveyor Motor (CONV-L3-MTR-01)

23 records, Apr 2025 – Aug 2026; aliases seeded: `L3 conv`, `CONV L3 MTR`,
`conveyor motor line 3` (NOT `Conv Motor-3` — that one is learned live in the demo).
Bearing replacements on an exact ~92-day cycle (the recurrence fixture):

| date | mode | raw text (verbatim style) | part | hrs | tech |
|---|---|---|---|---|---|
| 2026-08-14 | Bearing | `MTR brng noise L3 conv, replcd 6205ZZ, algnmnt chk, OK` | 6205ZZ | 2.0 | Sunil |
| 2026-05-14 | Bearing | `brng noise again L3 conv mtr. 6205ZZ replaced. algnmnt checked ok` | 6205ZZ | 2.4 | Sunil |
| 2026-02-11 | Bearing | `CONV L3 MTR BRG noise. bearing chng 6205ZZ. ok` | 6205ZZ | 2.5 | Sunil |
| 2025-11-11 | Bearing | `bearing gaya L3 conveyor motor. naya brg 6205ZZ lagaya. 3 hr gaya` | 6205ZZ | 3.0 | Rahul |
| 2026-01-25 | VFD | `CONV TRIP L3 VFD` | — | 1.4 | Amit |
| 2026-03-19 | Alignment | `Motor vibration L3 conv. alignment kiya dial gauge se` | — | 1.8 | Vijay |
| + Coupling ×2, Overheating ×2, Electrical trip ×1, Other ×1, PM/inspection ×11 | | | | |

Expected derived numbers (test assertions): 23 records · 12 breakdowns · 18.9 h downtime ·
MTTR 1.6 h · Pareto Bearing 4 / Overheating 2 / Coupling 2 / Alignment 1 / VFD 1 / Electrical 1
/ Other 1 · recurrence avg interval ≈ 92 days.

## 3. Other PDF-anchored records

- **Blister Machine 2** (27 records) — temporary-fix saga:
  `Blister m/c 2 stopping intermittent. sensor cleaned. chalu.` (2026-07-05) →
  `blister 2 phir se ruk raha. sensor clean kiya. baad me again stopped.` (2026-07-19) →
  `Blister m/c 2 stopping intermittent. proximity sensor changed. running.` (2026-08-06)
  → fixture for TEMP_FIX_REPEAT.
- **Packing Machine 4** — `packing m/c 4 ka belt slip ho raha tha, tension diya, phir bhi slip,
  belt change kiya, 1.5 hr gaya` (V-Belt A42).
- **Pump P-201** — `Pump P-201 seal leak again 3rd time this yr. Gland packing replaced.
  Yadav sir bolte hai shaft me runout hai. Check karna padega.` → technician-hypothesis fixture.
- **6205ZZ distribution** (PART_CONCENTRATION fixture): conveyor motor 4 + L3 blower 2 +
  Compressor #2 4 = 10 uses, Line 3 share 60 %.
- **Line-3 bearing downtime** (STAT_QUERY fixture): 7 bearing breakdowns on Line 3 in 2 years
  totalling **18.7 h** (motor 9.9 + blower 4.6 + gearbox 4.2), 7/7 with valid downtime.

## 4. Import fixture file (`fixtures/maintenance_log_2023.xlsx`)

1,842 data rows across 12 monthly sheets:
- 52 skip rows (empty / duplicate / header artifacts) with expected reasons,
- **37 rows with machine text `Conv Motor-3`** (unresolvable until alias mapped),
- 6 assorted low-confidence rows (incl. bare `CONV TRIP L3 VFD` and an approximate-downtime
  compressor row), → expected outcome: 1,747 auto-approved · 43 review · counts match the
  demo script exactly,
- row 143 of sheet March-2023 = the canonical `MTR brng noise…` line (citation example).

## 5. Live-demo objects (created during the demo, not seeded)

Record **#2048** (WhatsApp: bearing replaced, 2 h, technician Ramesh) and the resulting
RECURRENCE pattern update ("5th bearing replacement") — the seed must leave the state exactly
one confirmation away from these moments.

## 6. Patterns pre-seeded

RECURRENCE (conveyor motor, DETECTED) · TEMP_FIX_REPEAT (Blister 2, DETECTED) ·
PART_CONCENTRATION (6205ZZ, DETECTED) · DOWNTIME_HOTSPOT (Line 3, DETECTED) — each with full
evidence rows pointing at the seeded records. None CONFIRMED (that transition is the
engineer's demo action).

## 7. Format

Seed defined as YAML/JSON resources consumed by the loader (idempotent, upsert-by-code), not
hand-written SQL — same fixtures drive tests and demo environments so numbers can never drift
between docs, tests and the stage. (SQL migrations remain schema-only.)
