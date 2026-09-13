# 08 — Search Architecture

Goal: `bearing noise conveyor`, `brng noise conveyor`, `bearing gaya`, `CONV TRIP L3`,
`6205 bearing`, `L3 conveyor motor` must all find the right machines/records — inside
PostgreSQL (no Elasticsearch in V1; see doc 27).

## 1. Layers of one hybrid query

```
User query
 ↓ (a) NORMALIZATION & EXPANSION            deterministic, shared dictionary
     lowercase, strip punctuation, expand shorthand/synonyms:
     brng|brg|bearng|बेयरिंग|बेअरिंग → bearing · m/c → machine · conv → conveyor ·
     "bearing gaya" → bearing failure · L3 → line 3 …
 ↓ (b) ENTITY PECKING                        deterministic
     part-code pattern (6205ZZ, A42) → parts index
     machine-ish tokens → MachineResolverService (exact→alias→trigram→semantic)
 ↓ (c) KEYWORD LEG                           Postgres
     tsvector FTS (websearch_to_tsquery on expanded terms)
     + pg_trgm similarity on raw_text for shorthand not in dictionary
 ↓ (d) SEMANTIC LEG                          pgvector
     embed(query) → HNSW cosine top-k over record_embeddings (plant-filtered)
 ↓ (e) FUSION & EXPLANATION
     Reciprocal Rank Fusion of (c)+(d); metadata boosts (resolved machine, matching
     failure mode, shared part, recency); attach why[] per hit:
     KEYWORD_MATCH · SHORTHAND_VARIANT · ALIAS_MATCH · SAME_FAILURE_MODE ·
     SHARED_PART · SEMANTIC_SIMILARITY
 ↓ (f) SQL FILTERS applied throughout (never post-hoc): plantId (mandatory scope),
     lineId, machineId, failureModeId, source, dateFrom/dateTo, kind
```

## 2. What happens for each example query

| Query | Path |
|---|---|
| `bearing noise conveyor` | expansion no-op → FTS hits + semantic hits; conveyor boosts machine-type match |
| `brng noise conveyor` | (a) brng→bearing → identical result set as above; why includes SHORTHAND_VARIANT |
| `bearing gaya` | dictionary maps Hinglish phrase → bearing failure; semantic leg independently matches "bearing gaya L3 conveyor motor…" |
| `CONV TRIP L3` | (b) resolver: "conv l3" → Line 3 Conveyor Motor (alias); FTS finds raw "CONV TRIP L3 VFD"; failure-mode VFD boost |
| `6205 bearing` | part regex → part 6205ZZ → part hit + records joined via record_parts + machines via usage |
| `L3 conveyor motor` | resolver exact/alias → machine hit first; records filtered to that machine |

## 3. Indexing pipeline

```
MaintenanceRecordCreated / Updated (event)
 ↓ synchronous: search_tsv generated column updates automatically (Postgres)
 ↓ async (jobs: EMBEDDING_BACKFILL or per-record task):
   text = raw_text + normalized action + machine name + failure mode + parts
   → EmbeddingClient → record_embeddings upsert (model + dim recorded)
Rejected/soft-deleted records: embedding row deleted; FTS excluded by deleted_at filter.
Bulk import: embeddings generated in batches after approval (not per-row during pipeline).
```

## 4. Machine resolution inside search

Same `MachineResolverService` as ingestion/assistant (single implementation — doc 11):
exact code/name → alias table (normalized) → trigram similarity ≥0.45 → embedding
similarity of machine name/alias strings. Search never auto-creates aliases.

## 5. Ranking details

- RRF: `score = Σ 1/(60 + rank_leg)`; legs: FTS rank, trigram sim rank, vector cosine rank.
- Boosts (additive, small): resolved-machine records ×, failure-mode term match, shared part,
  recency decay (half-life 18 months) — recency never outranks exact content match.
- Thresholds: drop vector hits with cosine < 0.55; drop trigram < 0.3; cap merged k=50 before pagination.

## 6. API surface

- `GET /api/v1/search` — omni (machines/parts/records/patterns groups, top 5 each).
- `GET /api/v1/search/records` — paginated record search with filters + why[] (used by
  Maintenance History UI and by the assistant's `search_records` tool — one code path).
Both defined in doc 03 §10.

## 7. Performance envelope & future

- 10⁵–10⁶ records/plant: FTS+GIN and HNSW comfortably < 100 ms; queries always carry plant_id.
- FUTURE (doc 27): OpenSearch/Elasticsearch only if multi-plant corpus > ~10⁷ records or
  advanced relevance tuning is needed; the SearchService port isolates that swap.
