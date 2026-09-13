# LogSense — First-Time Founder Guide

Practical points to take care of before and while turning LogSense from a demo + blueprint
into a business. Written for a first startup, ordered by what can actually kill the idea.
(Business guidance, not legal/tax advice — points marked ⚖️ need a CA/lawyer in India.)

---

## 1. Validate demand BEFORE building the backend (highest risk)

The demo looking real is not the same as someone paying for it. The riskiest assumptions are:

1. **Plants will hand over their maintenance data** to a new vendor.
2. **Someone owns the pain enough to pay** — usually the Maintenance Manager feels it, but the
   Plant Head or IT Head signs. Find out who signs in your target plants.
3. **The messy-data promise survives real files.** Your demo Excel is clean compared to reality.

Actions (do these before writing Spring Boot code):
- Run **10–15 discovery conversations** with maintenance managers/plant heads (use your network,
  LinkedIn, industry associations like CII/MCCIA in Pune). Ask about their last 3 breakdowns and
  how they diagnosed them — don't pitch first, listen first.
- Get **2–3 real historical files** (even anonymized) from friendly plants and manually check:
  can the pipeline you designed realistically extract machine/failure/downtime from them?
  This one exercise will teach you more than a month of coding.
- Convert one plant into a **paid design partner** (even a token ₹25k–₹1L pilot). A free pilot
  proves nothing; a paid pilot proves the pain is real. Give them pricing protection in return.

**Kill/continue criterion:** if after 15 conversations nobody will share a file or discuss a
paid pilot, change the wedge before building.

## 2. Measure the number the whole product depends on

LogSense's economics live or die on **auto-approval rate**: what % of real rows extract at
high confidence without human review. In the demo it's ~97% (1,747/1,790). On real data it
might be 60%. If every second row needs review, the "4 weeks, no effort" promise collapses.

- In every pilot, measure: auto-approval %, corrections per 100 records, time an engineer
  spends in the validation queue, and resolver accuracy after alias learning.
- Set an internal bar (e.g., ≥85% auto-approval after the first alias-mapping session) and
  treat pilots below it as product work, not sales work.

## 3. Trust is the product — never break it

Your differentiator vs. "just ask ChatGPT" is the trust architecture (deterministic numbers,
citations, hypothesis labels). Guard it commercially too:
- Never let a salesperson (including you) claim "AI finds root causes." It flags patterns;
  engineers confirm. One wrong "confirmed root cause" in front of a plant head ends the deal.
- Keep the "Demo Environment" honesty: never present demo numbers as real client results.
- When (not if) extraction makes a mistake in a pilot, show the correction workflow proudly —
  it's a feature, not an embarrassment.

## 4. Customer data: the thing plants will grill you on ⚖️

Maintenance history reveals production reliability — plants treat it as sensitive.
Be ready with answers *before* the first pilot:
- **NDA + Data Processing terms** signed before any file leaves their premises. State clearly:
  they own the data; you delete/return it on exit; you never train models on it or share it
  across customers.
- **India DPDP Act 2023**: technician names/phone numbers are personal data — you're processing
  them (WhatsApp identity especially). Have consent/purpose language ready.
- **LLM sub-processor disclosure**: your design sends record text to an LLM API. Plants will
  ask "does our data go to a foreign AI company?" Prepare the honest answer, offer the
  zero-retention/enterprise API option, and list sub-processors in the contract.
- Basic security story: encrypted at rest/in transit, per-plant isolation (your RBAC design),
  audit logs (you have this), backups. Write a 1-page security FAQ — it will unblock deals.
- WhatsApp: use the official Business API only (never unofficial gateways — plants and Meta
  both punish that), and get technician consent for the phone-number binding.

## 5. Company & IP hygiene ⚖️ (cheap now, expensive later)

- Incorporate (typically **Private Limited** for a fundable startup in India; an LLP is cheaper
  if you'll bootstrap) and register under **Startup India/DPIIT** for benefits.
- If a co-founder joins: **founders' agreement with 3–4 year vesting and a 1-year cliff**, in
  writing, on day one. The #1 first-startup killer is a 50/50 handshake that breaks.
- **IP assignment**: everything built (including this repo's work) should be assigned to the
  company once it exists, and every future employee/contractor signs IP assignment.
- **Trademark check the name "LogSense"** in India (Class 9/42) and check domain availability
  before printing it on anything. Similar-sounding marks exist in logging/software — verify
  early; renaming after 10 pilots is painful.
- If the Factory.pdf concept came from/with anyone else, get clarity in writing on who owns
  the idea/material.

## 6. Money: unit economics from day one

- **Know your cost per plant per month**: LLM tokens for a 2,000-row import + daily questions,
  WhatsApp conversation fees, hosting (~1 small VM + Postgres in your V1 design). The blueprint
  already tracks tokens per message — actually watch that number in pilots.
- Price on **value, not cost**: your own settings screen says downtime costs ₹1,25,000/line-hour.
  If LogSense saves one hour of diagnosis a month, a ₹20k–50k/month/plant subscription is
  defensible. Don't price like a ₹2k SaaS tool.
- Manufacturing sales cycles are 2–6 months with multiple stakeholders. Keep a pipeline of
  10+ conversations so one slow deal doesn't stall you; keep 12+ months personal runway math.

## 7. Competition & positioning

Your real competitor is **Excel + memory + "we manage"** — inertia, not another product.
Existing CMMS players (UpKeep, Fracttal, Facilio, MaintWiz, Cryotos in India) sell
"replace your process"; your wedge is the opposite: **"keep everything, we make your history
searchable in Hindi/English in 4 weeks."** Protect that wedge:
- Never drift into becoming a CMMS (work orders, scheduling, inventory) during pilots —
  your `docs/27-v1-vs-future.md` scope discipline is a business weapon, not just engineering.
- The Hinglish/shorthand/register capability is your moat in Indian plants — invest there
  (dictionaries, regional languages, OCR of handwritten registers) before generic features.

## 8. Pilot playbook (repeatable from day one)

For each pilot, agree in writing: success criteria (e.g., "answer 10 real historical questions
correctly with citations"), data provided, 4-week timeline, named champion, price of the pilot,
and price of the annual subscription if it succeeds. Run your existing 9-minute demo, then
their own data as week-1 proof. Collect a referenceable quote + case-study permission as part
of the deal.

## 9. Metrics that tell you the truth

Track weekly per plant: questions asked to the assistant, active engineers/technicians,
WhatsApp records created, validation queue burn-down, auto-approval %, time-to-first-answer
for a new user. **Usage of the assistant + WhatsApp entries = retention leading indicators.**
If engineers stop asking questions in week 3, the pilot is failing even if nobody says so.

## 10. What NOT to worry about yet

Consistent with `logsense-backend/docs/27-v1-vs-future.md`: no Kubernetes, Kafka,
microservices, multi-region, SOC2, fundraising decks, office, hiring beyond need, or a
mobile app. One VM, one database, one design partner, one repeatable pilot. Also: don't
polish the demo further — it's already better than what most seed startups show; spend the
time in plants.

## 11. First 90 days (suggested)

**Days 1–30 — Validate**: 15 discovery calls · collect 3 real data files · manual extraction
audit on them · trademark/domain check · draft NDA + security FAQ · define pilot offer & price.
**Days 31–60 — Prove**: sign 1–2 paid design partners · incorporate (if validation positive) ⚖️
· build backend Phases 1–7 (foundation → validation queue: enough to run a real import end-to-end)
· run first real import with the design partner sitting next to you; measure auto-approval %.
**Days 61–90 — Repeat**: Phases 8–10 (search, analytics, assistant) · first plant answering
real questions with citations · WhatsApp with 2–3 technicians · pricing conversation for the
annual contract · decide bootstrap vs. raise based on pilot pull.

## 12. Personal notes for a first startup

- Talk about the idea openly — ideas are cheap, plant relationships and execution are the moat.
  Secrecy costs you feedback; nobody will out-execute you at your own customer's site.
- Write down every pilot learning the same week; your future pitch and product roadmap are
  those notes.
- Expect the first version of the pitch, price, and even the wedge to be wrong; the process
  above exists to find that out cheaply.
- Take care of yourself: manufacturing sales means factory visits, early mornings and slow
  months — pace like a marathon.

---

*Repo context: the clickable demo lives in `logsense-demo/`, the full backend specification in
`logsense-backend/` (start implementation only after design-partner validation — the roadmap's
Phases 1–7 are exactly the "run one real pilot" slice).*
