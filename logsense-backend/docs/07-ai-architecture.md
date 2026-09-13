# 07 — AI Architecture

**The prime directive (PDF trust principle, Rule 1):** the LLM never invents operational
numbers. Every figure a user sees was computed by SQL/deterministic code; the LLM's job is
language — understanding it and producing it — around injected, verified data.

## 1. Division of responsibility

| Deterministic backend (SQL / Java) | LLM |
|---|---|
| Downtime totals & averages | Understanding natural language questions |
| Failure counts, frequencies, recurrence intervals | Hinglish / Hindi / Marathi / technician shorthand |
| Pareto, KPI (MTTR/MTBF), percentages, trends | Query interpretation → structured intent |
| Parts usage & shares | Extraction of fields from messy raw text (ingestion/WhatsApp) |
| Machine statistics, date math | Summarization of retrieved records |
| Pattern metrics & evidence sets | Explanation in the user's language |
| Citation record sets | Hypothesis generation (always labelled HYPOTHESIS) |

Consequence: analytics answers are **reproducible** (same data → same numbers) and the LLM
can be swapped without changing any number anywhere.

## 2. Answer pipeline (assistant/query)

```
Question ("Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?")
   ↓ 1. QUERY UNDERSTANDING (LLM, JSON-schema-constrained output)
      → {intent: MACHINE_HISTORY, machineText:"conveyor motor line 3",
         dateRange:{from:-24m}, language:"hi-Latn"}
   ↓ 2. MACHINE RESOLUTION (deterministic MachineResolverService — same one as ingestion)
      → machineId CONV-L3-MTR-01, confidence 0.97
   ↓ 3. TOOL EXECUTION (deterministic; whitelisted, read-only)
      machine_stats(machineId, range)        → SQL numbers
      search_records(query, machineId, k=12) → hybrid retrieval (doc 08)
      pattern_lookup(machineId)              → detected patterns + metrics
   ↓ 4. ANSWER COMPOSITION (LLM)
      Prompt contains ONLY: tool JSON outputs + retrieved record texts + block schema.
      Instruction: "Use numbers verbatim from TOOL_RESULTS. If a needed number is absent,
      say it is unavailable. Mark any causal speculation as a HYPOTHESIS block."
      → typed blocks (SUMMARY / KEY_FAILURES / STATISTICS / PATTERN / HYPOTHESIS)
   ↓ 5. POST-VALIDATION (deterministic guardrail)
      • every numeral in FACT/STATISTICS blocks must exist in tool outputs (string/rounded match)
        → violation: block rejected, tool value substituted or block dropped, incident logged
      • HYPOTHESIS block forced if LLM emitted causal language outside one
      • FACT block without a mappable citation → downgraded to omitted
   ↓ 6. CITATIONS (CitationBuilder)
      retrieved record IDs + stats coverage set → ai_citations rows
   ↓ persist ai_message (blocks, tools_used, model, tokens, latency) → respond
```

### Intent taxonomy (V1 — closed set)

`MACHINE_HISTORY · STAT_QUERY (downtime/count/MTTR by filters) · REPEATED_FAILURES ·
PART_USAGE · TOP_MACHINES · SIMILAR_FAILURES · RECORD_SEARCH · PATTERN_EXPLAIN ·
SMALL_TALK/OUT_OF_SCOPE (polite refusal listing capabilities)`

### Tool registry (all deterministic, plant-scoped, read-only)

| Tool | Backing service | Returns |
|---|---|---|
| `resolve_machine(text)` | MachineResolverService | candidates + confidence |
| `machine_stats(machineId, range)` | MachineStatsService | records/breakdowns/downtime/MTTR/MTBF/Pareto |
| `stat_query(filters)` | AnalyticsService | aggregate for arbitrary filter combo + coverage counts |
| `search_records(q, filters, k)` | SearchService | scored records + why[] |
| `part_usage(partCode, range)` | PartUsageService | machines/counts/shares/interval |
| `top_machines(metric, k)` | AnalyticsService | ranked list |
| `pattern_lookup(machineId?)` | PatternRepository | fact metrics + hypothesis text |
No tool executes free-form SQL. The LLM selects tools + arguments (function-calling); the
executor validates arguments (plant scope, date sanity, k ≤ 20) before running.

## 3. Grounding & trust contract (response schema)

Every block carries a `label`:
- `FACT` — restates retrieved records; must have ≥1 citation.
- `CALCULATED` — numeric; must carry `tool` + `coverage` (e.g., "7 records · 7/7 valid downtime").
- `HYPOTHESIS` — speculation; must carry `confidence: POSSIBLE|LIKELY` + fixed disclaimer
  "requires engineering validation"; never contains new numbers.
The frontend renders badges directly from labels — trust is a wire-format property, not a UI choice.

## 4. Extraction (ingestion & WhatsApp use of the LLM)

`NormalizationService` calls the LLM with a strict JSON schema per raw row:
```
in : "MTR brng noise L3 conv, replcd 6205ZZ, algnmnt chk, OK"
out: {date:null, machineText:"L3 conv", failureMode:"Bearing", action:"Bearing replaced",
      parts:["6205ZZ"], downtimeHours:null, technician:null, kind:"BREAKDOWN",
      fieldConfidence:{machineText:0.95, failureMode:0.97, …}}
```
- Batched (≈20 rows/request) for cost; retried with backoff; rows failing twice → status FAILED (retryable via API 8.9).
- Confidence calibration: LLM self-score × resolver score × field-completeness → `overall_confidence`;
  `≥ plant.auto_approve_threshold` → auto-approve, else validation queue. Thresholds per plant.
- Language: Hinglish/Devanagari handled natively by the model; shorthand dictionary
  (brng→bearing, m/c→machine, "2 ghante"→2.0 h) applied both pre-prompt (normalization hints)
  and in search expansion, so behavior matches between extraction and retrieval.

## 5. Providers & configuration

Pluggable `LlmClient` port; default implementation: **Anthropic Claude API**.

| Task | Default model (configurable) | Why |
|---|---|---|
| Query understanding + answer composition | `claude-sonnet-5` | quality on Hinglish + tool use |
| Bulk row extraction / WhatsApp turns | `claude-haiku-4-5-20251001` | cost/latency at 1000s of rows |
| Embeddings | pluggable `EmbeddingClient` (e.g., a multilingual embedding provider), dim recorded per row | Claude API does not serve embeddings |

Config via env (`LLM_PROVIDER, LLM_API_KEY, LLM_MODEL_ANSWER, LLM_MODEL_EXTRACT,
EMBEDDING_PROVIDER, EMBEDDING_MODEL, EMBEDDING_DIM`). Timeouts 30 s, circuit breaker;
on LLM outage: assistant returns deterministic blocks with 424 semantics (doc 03 §11.1),
ingestion pauses at NORMALIZE stage and resumes via retry.

## 6. Cost, latency, safety

- Token in/out + latency persisted per `ai_message` and per extraction batch (doc 19 metrics).
- Prompt-injection posture: raw record text and WhatsApp content are **data**; system prompt
  instructs the model to treat retrieved text as quoted material; tools are whitelisted and
  read-only, so a hostile log line cannot mutate state; answers are post-validated (step 5).
- PII: technician names only; no special category data. Logs redact message bodies at INFO.

## 7. Assistant API surface

Defined in doc 03 §11 (`/assistant/query`, conversations, citations, feedback). Conversation
context = last N turns' intents + resolved entities (not raw transcripts) re-injected into
query understanding, so follow-ups like "aur is saal?" resolve against the prior machine.

## 8. What is deliberately absent in V1

- No free-form text-to-SQL (numbers only via whitelisted tools).
- No autonomous actions (the assistant never writes data).
- No model fine-tuning; corrections improve the **resolver and dictionaries**, not the LLM.
- No streaming responses (simple request/response first; SSE is a FUTURE line item).
