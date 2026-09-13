# 27 — V1 vs Future (scope control)

Recommended architecture: **modular monolith** Spring Boot + PostgreSQL(+pgvector) + object
storage + two external AI providers + WhatsApp provider. One deployable, one database.
Reasoning: V1 serves single-org, few plants, ≤ low-millions of records, tens of concurrent
users. Every "enterprise" addition below must earn its operational cost.

## V1 — MUST HAVE

| Capability | Notes |
|---|---|
| Auth (JWT+refresh), users, per-plant RBAC (5 roles) | doc 16 |
| Plant / line / machine master + plant settings | Area excluded |
| Failure-mode taxonomy + parts catalog | |
| Maintenance records with immutable raw provenance | Rule 5 backbone |
| File ingestion: xlsx/csv/pdf/scans, mapping, async pipeline, retry, duplicates | doc 09 |
| LLM normalization (Hinglish/shorthand extraction) with confidence | doc 07 §4 |
| Machine resolution + alias learning | doc 11 |
| Validation queue incl. alias-group bulk mapping | doc 10 |
| Hybrid search (FTS + trigram + pgvector) with why[] | doc 08 |
| AI assistant: grounded blocks, citations, hypothesis labels, guardrail | doc 07 |
| Deterministic analytics (KPIs, trend, Pareto, top machines, part usage) | doc 15 |
| Pattern detection (5 detectors) + human-only CONFIRM review | doc 12 |
| WhatsApp entry (text + confirmation flow; voice hook) | doc 14 |
| Dashboard composite | doc 15 |
| Audit logs, jobs framework, error model, observability baseline | docs 17–19 |

## V1 — NICE TO HAVE (build if schedule allows; nothing else depends on them)

- In-app notifications (module O) — dashboard already surfaces the same facts.
- Reports export (CSV records, monthly PDF) — module Q.
- Assistant feedback endpoint & conversation titles.
- ClamAV upload scanning hook; Grafana dashboard JSONs; OpenAPI-diff CI gate.
- Jobs list endpoint (`GET /jobs`) beyond single-job polling.

## FUTURE (explicitly out of V1 — with the trigger that would justify each)

| Item | Trigger to revisit |
|---|---|
| **Kafka / broker + outbox** | a second deployable or external consumers of domain events |
| **Elasticsearch/OpenSearch** | corpus ≫ 10⁷ records or relevance tuning beyond RRF needs |
| **Microservices extraction** | team > ~8 devs or independent scaling of ingestion/AI paths; module boundaries + ports already shaped for it (Rule 8) |
| **Kubernetes** | more than a handful of instances; V1 runs on compose/one VM/managed container |
| **Redis** | multi-instance cache coherence or distributed rate limiting needed |
| Event sourcing / workflow engine | never for these flows; explicit state machines suffice |
| Multi-region / HA beyond backup-restore | contractual SLA demands it |
| SAP/CMMS write-back or live sync | customer asks; PDF's promise is "no SAP changes" |
| Voice transcription as bundled service | per-plant volume justifies the provider contract |
| Predictive/ML failure models, sensor ingestion | out of product positioning ("without a single sensor") |
| Multi-tenant SaaS layer (orgs above plants) | commercial model change |
| SSE/streaming assistant answers, mobile push | UX pull after V1 feedback |
| Dynamic permission matrix, SSO/SAML | enterprise procurement requirement |
| Data warehouse / BI export | analytics beyond operational dashboard |

## Guardrails that keep V1 honest (restate of doc rules)

1. Numbers only from SQL/deterministic code (NumericGuardrail enforced).
2. Every AI statement traceable (citations schema NOT NULL bridge).
3. Hypothesis ≠ Fact (wire-format labels + DB trigger on CONFIRMED).
4. Low confidence ⇒ human validation (threshold routing).
5. Raw data immutable (append-only tables, RESTRICT FKs).
6. `/api/v1`, uniform paging/filtering/errors.
7–8. Monolith now, extraction-ready boundaries.
9–10. No module, table, class or endpoint exists without a workflow that uses it —
   docs 03/04/21 cross-check enforced in the consistency audit.
