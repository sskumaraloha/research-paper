# 14 — WhatsApp Integration

Technicians report breakdowns in the language they already use; the agent structures it, asks
for what's missing, confirms, and files a fully traceable record. Provider-pluggable
(`WhatsAppClient` port; default: Meta WhatsApp Cloud API — a BSP adapter is a drop-in).

## 1. Endpoints

| API | Auth | Purpose |
|---|---|---|
| `GET /api/v1/integrations/whatsapp/webhook` | Public (verify token) | provider verification challenge |
| `POST /api/v1/integrations/whatsapp/webhook` | Public + **HMAC signature verified** | inbound messages/status; fast 200 ack, async processing |
| `GET /api/v1/whatsapp/conversations` (+`/{id}`) | ENGINEER | monitoring, transcript, resulting record link |
| `POST /api/v1/whatsapp/contacts` / `DELETE …/{id}` | PLANT_ADMIN | technician phone ↔ user/plant binding |

Webhook rules: reject on bad signature (401, logged); idempotent on `provider_message_id`
(unique column); unknown phone → single polite refusal template, **zero data access**;
payload stored verbatim in `whatsapp_messages.payload`.

## 2. Conversation state machine (ConversationEngine)

```
inbound message → resolve contact (phone → user → plant)
STATE
NEW                → greet or treat first message as report
EXTRACTING         → NormalizationService on message text (same extractor as ingestion)
                     required fields for a WhatsApp breakdown: machine, issue, action, downtime
AWAITING_FIELD     → one question per missing field, in the technician's language
                     ("Machine kitni der band thi?"); field answers merged into draft
AWAITING_CONFIRMATION → structured echo + interactive buttons [Confirm] [Change]
CONFIRMED          → staged_record(source=WHATSAPP, resolution recorded)
                     → auto-approve path → maintenance_record
                     → reply "Record #… created ✅" (+ recurrence note if pattern fires)
ABANDONED          → no reply for 24 h (expires_at) → gentle timeout message, draft kept
```
- "Change" → agent asks which field; free-text corrections re-extracted.
- Multiple reports in one conversation: after CONFIRMED, state resets to NEW.
- Machine resolution below threshold → agent asks a disambiguation question with the top 3
  candidates as buttons (never guesses silently).
- Duplicate suspect (same machine/date/similar text) → record still created only after an
  explicit extra confirmation ("Aisi hi entry aaj already hai — phir bhi save karun?").

## 3. Worked example (PDF flow)

```
Tech: "Line 3 ka conveyor motor band tha, bearing change kiya, alignment check kiya, ab chal raha hai."
 → extract: machine="line 3 conveyor motor"(resolved 0.97), issue=Bearing failure,
   action=Bearing replaced + alignment checked, downtime=∅
Agent: "Samajh gaya 👍 … Machine kitni der band thi?"
Tech: "2 ghante"                       → downtime = 2.0 h
Agent: [structured echo] "Confirm?"    → [Confirm]
 → record #2048 created (raw_record = conversation reference)
Agent: "Record #2048 ban gaya ✅. Note: Nov 2025 se is motor par 5th bearing replacement —
        engineering review ke liye flag kiya."
```
The recurrence note is triggered by the targeted pattern check on `MaintenanceRecordCreated`
(doc 12 §2) — deterministic, not LLM improvisation.

## 4. Voice notes

```
inbound AUDIO → media fetched via provider API → stored (media_key)
 → TranscriptionClient (pluggable external provider; language hint hi/mr/en)
 → transcript column → same EXTRACTING path as text
Low transcription confidence → agent echoes transcript and asks for confirmation before extracting.
```
V1 ships the hook + storage; the transcription provider is configuration
(`TRANSCRIPTION_PROVIDER`, may be disabled → agent replies "text bhejein please").

## 5. Traceability

`maintenance_record.raw_record_id → raw_records{source_type:WHATSAPP,
source_ref:{conversationId, messageIds[]}}` — the source drawer for a WhatsApp record shows
the exact confirmed transcript. Conversation, messages, and outbound replies are append-only.

## 6. Security & operations

- Signature verification mandatory; verify-token for GET challenge; webhook path rate-limited.
- Media validated (type/size ≤16 MB) before storage; media keys are private storage paths.
- Outbound only via templates/replies within the 24 h session window (provider policy).
- Send failures retried with backoff; conversation never blocks the webhook thread
  (queue → async worker).
- Metrics: inbound count, extraction success rate, avg turns-to-confirm, abandonment rate (doc 19).
