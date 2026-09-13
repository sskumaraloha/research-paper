# 24 — Testing Strategy

Test pyramid for a modular monolith: fast unit core, Testcontainers-backed integration ring,
thin E2E crown. No test code here — scope and critical scenarios only.

## 1. Unit tests (JUnit 5 + Mockito) — the bulk

| Target | What must be proven |
|---|---|
| `TextNormalizer` / `QueryExpansionService` | brng/BRG/bearng/बेयरिंग → bearing; m/c→machine; "2 ghante"→2.0; punctuation/case; idempotence |
| `MachineResolverService` | cascade order, line-token hard filter (l3 never matches Line 2), confidence bands, tie handling |
| `ConfidenceScorer` | threshold routing (auto vs review) incl. per-plant thresholds |
| `KpiCalculator` / `MachineStatsService` math | MTTR/MTBF/deltas on synthetic fixtures; empty windows; zero-downtime rows |
| Detectors (5) | recurrence interval math (92-day fixture), CV cutoff, temp-fix window, fingerprint stability, dismiss suppression |
| `NumericGuardrail` | rejects invented numerals; forces HYPOTHESIS labelling; drops uncited FACT blocks |
| `ColumnMappingService` | header synonym suggestion; minimum-mapping rule |
| `DuplicateDetector` | file hash, row hash, cross-import suspect similarity |
| `ConversationEngine` | state transitions, missing-field selection, timeout, double-confirm idempotence |
| JWT/`AuthService` | expiry, rotation, reuse-revocation |
| Validators/mappers/error mapping | envelope shape, code stability |

## 2. Repository tests (`@DataJpaTest` + **Testcontainers Postgres with pgvector**)

- Flyway migrations apply cleanly from empty (CI gate).
- Constraint behavior: alias uniqueness per plant, staged↔raw 1:1, record→raw NOT NULL,
  pattern CONFIRMED trigger rejection, soft-delete default filters.
- Aggregate queries: stats/analytics native queries against seeded fixtures with known answers.
- FTS + trigram + HNSW queries return expected ordering on the doc-25 dataset.

## 3. Integration / API tests (`@SpringBootTest` + MockMvc/WebTestClient, Testcontainers; LLM/embedding/WhatsApp clients replaced by deterministic fakes)

Per module: happy path + auth failure + validation failure + not-found + conflict for every
endpoint family in doc 03. Contract assertions on the error envelope and pagination wrapper.

## 4. Security tests

- Role matrix sweep: every endpoint × every role (expected 2xx/403/404) generated from
  `API-MASTER-INDEX` — the matrix is executable documentation.
- Plant isolation: user of plant A requesting plant B resources → 404 + audit row.
- Token lifecycle: expired access, rotated-refresh reuse → family revocation.
- Upload abuse: oversize, wrong magic bytes, xlsx XXE payload, path-traversal filename.
- Webhook: bad signature 401, replayed provider_message_id idempotent.
- Sort/filter injection attempts rejected by whitelists.

## 5. Ingestion pipeline tests (the critical suite)

Golden-file fixtures: a 1,800+-row xlsx modeled on the PDF (incl. Hinglish rows, shorthand,
37 × "Conv Motor-3", empty/duplicate rows, an unparseable row), a CSV, a text-layer PDF, a
scanned image (OCR fake).

**Critical scenario (end-to-end of the demo promise):**
```
Upload 1,842-row xlsx
 ↓ parse → detected 1842, skipped 52 (with reasons)
 ↓ confirm suggested mapping
 ↓ process (fake LLM extractor with recorded outputs)
 ↓ 1,747 auto-approved · 43 validation items · 37 share group_key "conv motor-3"
 ↓ POST alias-groups/map → 37 records created, alias learned
 ↓ re-import same file → 409 DUPLICATE_FILE; re-import variant file → 37 rows now auto-resolve via new alias
 ↓ machine history count and /machines/{id}/stats match expected fixture numbers exactly
```
Plus: cancel mid-run resumes cleanly; retry re-runs only FAILED rows; counts jsonb always
sums to detected.

## 6. AI tests (fakes + guardrail focus — no live LLM in CI)

- QueryUnderstanding fake returns fixture intents; assert tool selection & argument validation
  (out-of-scope plant rejected).
- **Grounding test**: composer fake emits an answer containing a number absent from tool
  results → NumericGuardrail strips/repairs + increments violation metric.
- Hypothesis labelling: causal text outside HYPOTHESIS block → forced/blocked.
- Citation integrity: every FACT block cites resolvable record IDs; citations round-trip via
  `GET /messages/{id}/citations` → `/maintenance-records/{id}/source`.
- Determinism: STAT_QUERY answers byte-equal across runs with fixed fixtures.
- Optional nightly (non-CI) smoke against the real LLM with the golden question set
  ("Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?" …) asserting schema validity,
  not wording.

## 7. Search tests

Doc-08 example queries against the doc-25 dataset: each must hit its expected records with
expected `why[]`; zero-result behavior; plant isolation of vector search; RRF ordering stable.

## 8. WhatsApp tests

Full scripted conversation (report → downtime question → "2 ghante" → confirm → record
created, source drawer shows transcript); disambiguation branch; abandonment timeout;
duplicate-day double-confirm; voice-note path with transcription fake.

## 9. End-to-end (few, high-value; Testcontainers app + HTTP client)

1. **Demo journey**: login → upload → process → alias map → machine stats → assistant question
   (fake LLM) with citations → WhatsApp record → recurrence pattern DETECTED → engineer
   CONFIRM with note → dashboard reflects everything.
2. Admin journey: create plant→line→machine→user→technician phone → manual record → audit trail complete.
3. Degradation: LLM fake set to fail → import pauses (retryable), assistant 424/degraded per doc 17 §5.

## 10. Non-functional & tooling

- ArchUnit: module boundary rules (no cross-module repository imports; controllers don't
  touch repositories).
- Load smoke (Gatling, pre-release): 50 rps mixed read on 10⁶-record dataset, P95 targets
  (search < 300 ms, stats < 200 ms, dashboard < 500 ms cached).
- Coverage gate: line 80 % on services, 100 % on NumericGuardrail, resolver cascade, KPI math.
- CI order: unit → repository → integration → security matrix → pipeline suite → E2E.
