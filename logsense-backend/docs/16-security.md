# 16 — Security

## 1. Roles (only what the product needs)

| Role | Scope | Justification (PDF personas) | Can |
|---|---|---|---|
| `SUPER_ADMIN` | global | vendor/IT owner | everything; create plants; manage all users |
| `PLANT_ADMIN` | per plant | Maintenance Manager / IT head of a plant | users & roles of the plant, settings, taxonomy, plus everything below |
| `ENGINEER` | per plant | Reliability/maintenance engineer | imports, validation, alias mapping, record corrections, pattern review, reports, plus below |
| `TECHNICIAN` | per plant | Shop-floor technician | create manual records, WhatsApp entry, read machine history & search & assistant |
| `VIEWER` | per plant | Plant Head, guests | read-only: dashboard, analytics, machines, history, search, assistant |

Hierarchy: `VIEWER < TECHNICIAN < ENGINEER < PLANT_ADMIN < SUPER_ADMIN` (higher includes lower).
No dynamic permission matrix in V1 — fixed enum, mapped to Spring Security authorities.

## 2. Authentication

- **JWT access tokens**, 15 min TTL, HS512 or RS256 (RS256 if any second consumer appears).
  Claims: `sub` (userId), `plantRoles` map, `superAdmin` flag, `jti`, `iat/exp`. No PII beyond name.
- **Refresh tokens**: opaque 256-bit random, stored **hashed** (`refresh_tokens.token_hash`),
  30 d TTL, **rotation on every use**; reuse of a rotated token ⇒ token-family revocation
  (stolen-token defense). Logout revokes; password change / deactivation revokes all.
- Passwords: **BCrypt cost 12**; policy ≥10 chars + breach-list check; generic 401 on failure
  (no user enumeration); login rate-limited (5/min/IP + 10/min/account).
- WhatsApp identity: phone-number binding table + webhook HMAC — never trust sender text
  claiming an identity.

## 3. Authorization model

```
Request → JwtAuthFilter (validate, load principal)
        → @PreAuthorize role check (method level)
        → PlantAccessService.check(userId, plantId, minRole)   ← every service entrypoint
        → repository queries ALWAYS filtered by plant_id
```
- Plant scoping is enforced in the **service layer with plant-filtered queries**, not just
  annotations — a missing check fails closed (resource not found).
- Cross-plant access attempts return **404** (not 403) to prevent resource-ID probing;
  the attempt is audit-logged.
- Ownership rules: AI conversations and notifications are owner-only regardless of role.
- Endpoint→role matrix lives in `docs/API-MASTER-INDEX.md` (single source; consistency-audited).

## 4. API & transport security

- HTTPS only (HSTS at proxy). CORS: explicit allow-list of frontend origins, credentials
  disallowed (bearer header, no cookies), max-age 1 h.
- Rate limiting (Bucket4j in-process for V1): global per-IP, stricter buckets for `/auth/*`,
  `/assistant/query` (LLM cost), and the WhatsApp webhook.
- Input validation: Bean Validation on every DTO (size caps, formats, enum whitelists);
  pagination caps (`size ≤ 200`); date-range caps (≤ 5 y); sort fields whitelisted per endpoint
  (no reflection-based sort injection). No dynamic SQL from user input anywhere (JPA/named
  native queries with parameters only) — including the assistant, which has **no** text-to-SQL.

## 5. File upload security (imports, WhatsApp media)

- Size caps (50 MB import / 16 MB media); extension **and** magic-byte content sniffing;
  xlsx parsed with entity-expansion disabled (XXE-safe factories); CSV formula-injection
  neutralized on any future export (`'` prefix for `=+-@`).
- Files stored outside the web root / in object storage with private ACL; served only through
  authenticated, audited download endpoints with `Content-Disposition: attachment` and a
  restrictive `Content-Type`. No user-controlled storage paths (server-generated keys).
- AV scan hook (ClamAV container) — V1-NICE, interface present.

## 6. LLM-boundary security

- Retrieved record text and WhatsApp content are untrusted data: system prompts mark them as
  quoted material; tools are whitelisted, read-only, argument-validated (doc 07).
- Prompt/response bodies logged only at DEBUG with redaction; API keys via env/secret store,
  never in DB or logs.
- Numeric post-validation (doc 07 §2.5) doubles as an integrity control against injected
  "instructions" inside maintenance text.

## 7. Sensitive data & privacy

- Stored personal data: user name/email/phone, technician names in historical records.
  No health/financial data. Phone numbers rendered masked outside admin screens.
- Deactivated users retained for attribution (soft delete); erasure request = anonymize
  name/email/phone, keep record linkage ids.
- Secrets: DB creds, JWT secret, LLM/WhatsApp keys via environment (12-factor); rotation
  documented in runbook.

## 8. Audit logging (see doc 05 `audit_logs`)

Audited: every login/logout/refresh-reuse, user/role changes, settings changes, imports,
validation actions, alias create/delete, record create/update/delete, pattern reviews, file
downloads, webhook rejections, cross-plant 404s. Entries carry actor, traceId, IP,
before/after JSON. Append-only, partitioned, admin-readable via `GET /audit-logs`.

## 9. Error security

Standard envelope (doc 17) never leaks stack traces, SQL, file paths, or internal class names;
`traceId` correlates with server logs instead. 401 vs 403 vs 404 semantics fixed as above.

## 10. Headers & misc

`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, minimal `Server` header,
`Cache-Control: no-store` on API responses; actuator: `/health` public-liveness only,
everything else on an internal port/network.
