# Maintenance Intelligence — React frontend

React 19 + Vite + React Router + Axios frontend for the **Maintenance Intelligence Platform API**.
[`openapi.json`](openapi.json) is the single source of truth for the backend contract.

- Contract analysis, API → UI mapping, open questions: [`docs/API_ANALYSIS.md`](docs/API_ANALYSIS.md)
- Per-endpoint inventory (generated from the spec): [`docs/API_INVENTORY.md`](docs/API_INVENTORY.md)

## Run

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173, proxies /api → VITE_PROXY_TARGET (default http://localhost:8080)
npm run build        # production build in dist/
```

No backend handy? `npm run mock` starts a contract mock on :8080 that serves every operation with
schema-shaped data and **rejects** anything outside the contract (unknown path/method, undocumented or
missing query params, body fields not in the schema, constraint violations, missing bearer token).

## Configuration

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend origin. Empty = same origin (`/api/...`). |
| `VITE_API_TIMEOUT_MS` | Request timeout (default 30000). |
| `VITE_PROXY_TARGET` | Dev server only: where `/api` is proxied. |

## Contract checks

```bash
npm run verify:api      # every METHOD + path + query param in src/ exists in openapi.json; every service is used by a page
npm run docs:inventory  # regenerate docs/API_INVENTORY.md from openapi.json
```

## How the code maps to the contract

```
src/api/client.js                 Axios instance: base URL, bearer token, refresh-on-401, error normalization
src/features/<domain>/services/   one function per operationId (METHOD + path documented on each)
src/utils/contract.js             request bodies built from components.schemas: only declared fields are sent,
                                  typed per schema, validated against maxLength/minLength/pattern/min/max/required
src/features/<domain>/pages/      pages; every API call goes through a service and renders loading/error/empty/data
```

State: auth and the selected plant live in React Context (`src/context`); everything else is page-local
server state via `useAsync`. No global store library.
