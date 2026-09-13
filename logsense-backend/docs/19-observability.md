# 19 — Observability

## 1. Logging

- JSON structured logs (logstash encoder) → stdout (12-factor). Levels: prod INFO, noisy
  libraries WARN.
- Every request log line carries: `traceId`, `requestId`, `userId` (if authed), `plantId`
  (when derivable), method, path, status, duration ms.
- **TraceId**: generated per request (or honored from `X-Request-Id`/W3C `traceparent`),
  stored in MDC, returned in the error envelope and as `X-Trace-Id` response header —
  the same id lands in `audit_logs.trace_id`.
- Redaction rules: never log passwords, tokens, raw LLM prompts/responses at INFO
  (DEBUG only, redacted), WhatsApp message bodies at INFO (ids only), file contents.

## 2. Metrics (Micrometer → Prometheus `/actuator/prometheus`, internal network only)

| Area | Metrics |
|---|---|
| HTTP | latency histograms per endpoint tag, error-rate by status/code |
| DB | HikariCP pool usage, slow-query log (>250 ms) |
| Ingestion | `import_rows_processed_total{stage,status}`, rows/sec, `import_jobs_active`, parse/normalize failure counters, per-import duration |
| Validation | `validation_open_items` gauge, oldest-open-item age, actions/day, auto-approval rate |
| Resolver | resolution attempts by method (EXACT/ALIAS/FUZZY/SEMANTIC), auto-resolution rate, correction rate |
| Search | query latency by leg (fts, trigram, vector), zero-result rate |
| **AI** | `llm_requests_total{task,model,outcome}`, latency histogram, `llm_tokens_total{direction}`, **estimated cost counter** (tokens × configured price), guardrail violations (`llm_numeric_violation_total`), 424 fallbacks |
| WhatsApp | inbound/outbound counts, signature failures, turns-to-confirm, abandonment rate |
| Patterns | scan duration, patterns detected/updated per run |
| Jobs | queue depth by type, job duration, failures |

AI cost tracking is dual: Prometheus counters for ops + per-message `tokens_in/out, model,
latency_ms` persisted on `ai_messages` (doc 05) for per-plant/product analysis.

## 3. Health checks

- `/actuator/health/liveness` (process) and `/readiness` (DB ping) — the only public
  actuator surface, used by the orchestrator/load balancer.
- Custom health contributors (internal detail view): object storage reachability, LLM
  provider (cheap ping, cached 60 s), embedding provider, WhatsApp provider token validity.
  Degraded upstreams report DEGRADED, not DOWN (app still serves deterministic features —
  matches degradation rules in doc 17 §5).

## 4. Error monitoring & alerting

- Uncaught exceptions → structured ERROR with traceId; optional Sentry/GlitchTip DSN hook
  (config-only, V1-NICE).
- Alert rules (Prometheus): 5xx rate > 1 % (5 m), import job FAILED, LLM error rate > 10 %,
  validation oldest-item age > 7 d, webhook signature failures spike, job queue depth stuck.

## 5. Import processing monitoring (product-level)

`GET /imports/{id}` is itself the user-facing monitor (stage, progressPct, per-status counts);
ops-side, each pipeline stage logs a per-batch summary line keyed by importJobId so a slow
import is diagnosable from logs alone. Job rows keep `started_at/finished_at` for throughput
history.

## 6. Dashboards (ops)

Grafana boards shipped as JSON (FUTURE-NICE): API overview, ingestion pipeline, AI cost &
latency, WhatsApp funnel. V1 requirement is only that the metrics above exist.
