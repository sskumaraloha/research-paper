# Phase 1: API Contract Analysis

Source of truth: [`../openapi.json`](../openapi.json), the *Maintenance Intelligence Platform API* v1 (OpenAPI 3.0.1).
It has 81 paths, 88 operations, 90 schemas and 18 controllers. The per-endpoint inventory (every parameter, body field,
constraint and response schema) is generated from the spec into [`API_INVENTORY.md`](API_INVENTORY.md).

## 0. What the existing repository contains

| Item | Finding |
|---|---|
| Existing React project | None. No `package.json` and no JS sources. The frontend is new, under `frontend/`. |
| Existing styling / design | None to preserve. The Thymeleaf templates in `src/main/resources/templates` belong to a different app (see below). |
| Backend in this repo | The Spring Boot app in `src/` is the *Store Management System* (e-commerce) and **does not implement** the Maintenance Intelligence API. The frontend targets `openapi.json` only. Nothing in `src/` was modified. |
| Dependencies to reuse | None. The new ones are listed in §5. |

## 1. Cross-cutting contract facts

| Concern | What the spec says | Frontend consequence |
|---|---|---|
| Authentication | Global `security: bearerAuth` (HTTP bearer, JWT). No per-operation overrides. | Every request sends `Authorization: Bearer <accessToken>`. The spec doesn't mark login, register and the other auth calls as public (see Q1), so the header is attached only when a token exists. |
| Token lifecycle | `AuthResponse { accessToken, refreshToken, expiresInSeconds, user: UserProfileResponse }` comes back from `login`, `register`, `refresh` and `demoLogin`. | Store both tokens. On a 401, call `POST /api/auth/refresh { refreshToken }` once, retry the request, and log out if that fails. |
| Logout | `POST /api/auth/logout { refreshToken }` returns 200 with no body. | Revoke server-side, then clear local tokens (even if the call fails). |
| Current user | `GET /api/auth/me` returns `UserProfileResponse { id, fullName, email, role, plantIds[] }`. | Called on app start to restore the session. |
| Plant scoping | Most read APIs require `plantId` (query or path). | A global plant selector backed by `GET /api/plants`, defaulting to the first plant in `user.plantIds` that is in the list. |
| Pagination | `PageResponse*` = `{ content[], page, size, totalElements, totalPages, first, last }`. Requests use `page` (0-based) and `size`. | One shared `<Pagination>` component and `page`/`size` query args. |
| Sorting | **No operation has a sort parameter.** | No sortable columns; rows appear in backend order. |
| Filtering | `listRecords`/`exportRecords`: machineId, lineId, failureModeId, from, to, text. `listMachines`: query, lineId, criticality. `listParts`: query. `list` (audit): plantId, action. `listSchedules`: includeInactive. Analytics: from, to, limit, months. | Filter bars build only these params. Empty values are omitted. |
| Deletion | **No DELETE operation exists.** | No delete buttons. Users use `deactivateUser`; schedules and machines use `active: false` via their PUT. |
| Record editing | No update operation for records, only `rejectRecord`. | Record detail is read-only apart from "Reject". |
| Error responses | **No error schema is documented for any operation.** | See Q2. The client normalizes errors defensively. |
| Response media type | Every response is `*/*`. `exportRecords` returns `string`. | JSON is parsed by default. Export is fetched as text and saved as a file. |

## 2. Feature / module list and API → service mapping

Each service function wraps exactly one operation, and each function name is the `operationId`, so it can be traced
back to the spec (`getConversation_1` becomes `getAssistantConversation`, because two services would otherwise
share a name).

| Feature (`src/features/…`) | Service file | Operations (operationId → METHOD path) |
|---|---|---|
| **auth** | `authService.js` | `login` POST /api/auth/login · `register` POST /api/auth/register · `refresh` POST /api/auth/refresh · `logout` POST /api/auth/logout · `me` GET /api/auth/me · `demoLogin` POST /api/auth/demo-login · `forgotPassword` POST /api/auth/forgot-password · `resetPassword` POST /api/auth/reset-password · `validateResetToken` GET /api/auth/reset-password/validate |
| **plants** (context + settings) | `plantService.js` | `listPlants` GET /api/plants · `listLines` GET /api/plants/{plantId}/lines · `getSettings` GET /api/plants/{plantId}/settings · `updateSettings` PUT /api/plants/{plantId}/settings |
| **config** | `configService.js` | `listFailureModes` GET /api/config/failure-modes · `getDictionary` GET /api/config/dictionary |
| **dashboard** | `dashboardService.js` | `plantKpis` GET /api/dashboard/kpis |
| **machines** | `machineService.js` | `listMachines` GET /api/machines · `createMachine` POST /api/machines · `getMachine` GET /api/machines/{machineId} · `updateMachine` PUT /api/machines/{machineId} · `getStats` GET …/stats · `getTimeline` GET …/timeline · `getInsights` GET …/insights · `recomputeInsights` POST …/insights/recompute · `listAliases` GET …/aliases · `addAlias` POST …/aliases |
| **records** | `recordService.js` | `listRecords` GET /api/records · `createRecord` POST /api/records · `getRecord` GET /api/records/{recordId} · `rejectRecord` POST /api/records/{recordId}/reject · `listFilterOptions` GET /api/records/filter-options · `listSourceDocuments` GET /api/records/source-documents · `exportRecords` GET /api/records/export |
| **entry** (conversational record entry) | `entryService.js` | `startConversation` POST /api/entry/conversations · `getConversation` GET /api/entry/conversations/{id} · `sendMessage` POST …/messages · `confirm` POST …/confirm · `requestEdit` POST …/request-edit |
| **imports** | `importService.js` | `uploadFile` POST /api/imports/upload · `getLatestJob` GET /api/imports/latest · `getJob` GET /api/imports/{jobId} · `rerunJob` POST /api/imports/{jobId}/rerun |
| **validation** | `validationService.js` | `getQueue` GET /api/validation/queue · `pendingCount` GET /api/validation/pending-count · `approve` POST /api/validation/{itemId}/approve · `reject` POST …/reject · `editAndApprove` POST …/edit-approve · `listAliasSuggestions` GET /api/validation/alias-suggestions · `mapAlias` POST …/{suggestionId}/map · `dismissAlias` POST …/{suggestionId}/dismiss |
| **schedules** | `scheduleService.js` | `listSchedules` GET /api/schedules · `listDue` GET /api/schedules/due · `createSchedule` POST /api/schedules · `updateSchedule` PUT /api/schedules/{scheduleId} · `completeSchedule` POST …/complete |
| **insights** | `insightService.js` | `listInsights` GET /api/insights · `insightCount` GET /api/insights/count · `recomputeAll` POST /api/insights/recompute |
| **analytics** | `analyticsService.js` | `downtimeTrend` · `topMachinesByDowntime` · `pareto` · `lineDowntimeShare` · `failureModeStats` · `partReplacementIntervals` (all GET /api/analytics/…) |
| **parts** | `partService.js` | `listParts` GET /api/parts · `getPartDetail` GET /api/parts/{partId} |
| **search** | `searchService.js` | `globalSearch` GET /api/search · `searchRecords` GET /api/search/records · `searchMachines` GET /api/search/machines |
| **assistant** | `assistantService.js` | `ask` POST /api/assistant/ask · `suggestions` GET /api/assistant/suggestions · `listConversations` GET /api/assistant/conversations · `getConversation_1` GET /api/assistant/conversations/{id} |
| **notifications** | `notificationService.js` | `listForUser` GET /api/notifications · `unreadCount` GET …/unread-count · `badgeCounts` GET …/badge-counts · `markRead` POST …/{id}/read · `markAllRead` POST …/read-all |
| **users** | `userService.js` | `listUsers` GET /api/users · `createUser` POST /api/users · `updateUser` PUT /api/users/{userId} · `deactivateUser` POST …/deactivate · `listRoles` GET /api/users/roles |
| **audit** | `auditService.js` | `list` GET /api/audit |
| **platform** | `platformService.js` | `overview` GET /api/platform/overview · `listOrganisations` GET /api/platform/organisations · `getOrganisation` GET /api/platform/organisations/{organisationId} |
| *(not in UI)* | — | `inbound` POST /api/webhooks/whatsapp is a server-to-server webhook (WhatsApp provider → backend, authenticated with `X-Webhook-Token`), so no browser screen should call it. |

That covers 87 of the 88 operations; the webhook is intentionally left out.

## 3. API → UI mapping

| Route | Page | Main components | Service calls |
|---|---|---|---|
| `/login` | LoginPage | LoginForm, demo button | login, demoLogin |
| `/register` | RegisterPage | form (RegistrationRequest) | register |
| `/forgot-password` | ForgotPasswordPage | form | forgotPassword |
| `/reset-password?token=` | ResetPasswordPage | form | validateResetToken, resetPassword |
| *(app shell)* | AppLayout | Sidebar with badges, PlantSelector, header search, NotificationBell, user menu | listPlants, badgeCounts, me, logout |
| `/dashboard` | DashboardPage | KPI tiles, trend chart, top-downtime bars, due schedules, top insights | plantKpis, downtimeTrend, topMachinesByDowntime, listDue, listInsights |
| `/machines` | MachinesListPage | filter bar (query, line, criticality), table, pagination | listMachines, listLines |
| `/machines/new`, `/machines/:machineId/edit` | MachineFormPage | Create/UpdateMachineRequest form | createMachine / getMachine + updateMachine, listLines |
| `/machines/:machineId` | MachineDetailPage | header, stats tiles, top failure modes, tabs: Timeline · Insights · Aliases | getMachine, getStats, getTimeline, getInsights, recomputeInsights, listAliases, addAlias |
| `/records` | RecordsListPage | filter bar, table, pagination, CSV export | listRecords, listFilterOptions, exportRecords |
| `/records/new` | RecordCreatePage | CreateRecordRequest form | createRecord, listFilterOptions, listFailureModes |
| `/records/:recordId` | RecordDetailPage | detail view, reject modal | getRecord, rejectRecord |
| `/entry`, `/entry/:conversationId` | EntryPage | chat thread, draft panel with missing fields, confirm / request edit | startConversation, getConversation, sendMessage, confirm, requestEdit |
| `/imports` | ImportsPage | upload form, latest job card, source documents table | uploadFile, getLatestJob, listSourceDocuments |
| `/imports/:jobId` | ImportJobPage | job summary, step pipeline, rerun | getJob, rerunJob |
| `/validation` | ValidationQueuePage | queue list, approve / reject / edit-and-approve modal | getQueue, approve, reject, editAndApprove, listFilterOptions, listFailureModes |
| `/validation/aliases` | AliasSuggestionsPage | suggestion table, map-to-machine / dismiss | listAliasSuggestions, mapAlias, dismissAlias, listFilterOptions |
| `/schedules` | SchedulesPage | tabs All / Due, create/edit modal, complete modal | listSchedules, listDue, createSchedule, updateSchedule, completeSchedule, listFilterOptions |
| `/insights` | InsightsPage | insight cards with evidence, recompute | listInsights, insightCount, recomputeAll |
| `/analytics` | AnalyticsPage | date-range filter, trend, pareto, line share, failure modes, top machines, part intervals | the six analytics operations |
| `/parts`, `/parts/:partId` | PartsListPage, PartDetailPage | search, table, pagination / detail | listParts, getPartDetail |
| `/search?query=` | SearchPage | tabs All · Records · Machines | globalSearch, searchRecords, searchMachines |
| `/assistant`, `/assistant/:conversationId` | AssistantPage | conversation list, answer renderer (tiles, sections, records, follow-ups), suggestions | ask, suggestions, listConversations, getConversation_1 |
| `/notifications` | NotificationsPage | list, pagination, mark read / mark all read | listForUser, markRead, markAllRead, unreadCount |
| `/users` | UsersPage | table, create/edit modal, deactivate confirm | listUsers, listRoles, createUser, updateUser, deactivateUser, listPlants |
| `/settings` | SettingsPage | plant thresholds form, lines, failure modes, dictionary | getSettings, updateSettings, listLines, listFailureModes, getDictionary |
| `/audit` | AuditLogPage | action filter, table, pagination | list |
| `/platform`, `/platform/organisations/:organisationId` | PlatformPage, OrganisationPage | overview tiles, org table / plants + users | overview, listOrganisations, getOrganisation |

The data flow on every page follows the same chain: the page or component calls a `features/*/services` function,
that goes to `api/client.js` (Axios, base URL from env, bearer header, refresh on 401, error normalization) and
then to the backend. The response is typed exactly as its schema and held in `useAsync` state, and the UI renders
one of loading, error, empty or data.

## 4. Project structure

```
frontend/
├── openapi.json                 # contract (source of truth)
├── .env.example / .env.development / .env.production
├── docs/API_ANALYSIS.md, API_INVENTORY.md
├── scripts/generate-inventory.mjs, verify-api.mjs
└── src/
    ├── api/            client.js, tokenStorage.js, errors.js
    ├── components/     common/ (Spinner, ErrorMessage, EmptyState, Pagination, Modal, ConfirmDialog,
    │                   FormField, PageHeader, Badge, StatTile, AsyncBoundary), charts/ (BarList, ColumnChart)
    ├── context/        AuthContext.jsx, PlantContext.jsx
    ├── hooks/          useAsync.js
    ├── layouts/        AppLayout.jsx, AuthLayout.jsx
    ├── routes/         AppRoutes.jsx, ProtectedRoute.jsx
    ├── features/<domain>/{services,pages,components}
    ├── utils/          format.js, forms.js, download.js
    ├── styles/         global.css
    ├── App.jsx
    └── main.jsx
```

## 5. Dependencies

| Package | Why |
|---|---|
| `react`, `react-dom` | UI |
| `react-router-dom` | Routing |
| `axios` | HTTP client with interceptors (token refresh, error normalization) |
| `vite`, `@vitejs/plugin-react` (dev) | Build and dev server |

No state-management library: auth and selected plant live in React Context, and everything else is page-local
server state loaded through `useAsync`. No UI kit or chart library; charts are small single-series SVG and CSS
components.

## 6. Implementation plan

1. **Foundation**: Vite scaffold, env config, API client, token storage, error normalization, Auth and Plant contexts, router with protected routes, app shell, common components.
2. **Features**, in dependency order: auth, dashboard, machines, records, imports, validation, entry, schedules, insights, analytics, parts, search, assistant, notifications, users, settings, audit, platform.
3. **Verification**: `scripts/verify-api.mjs` statically checks that every service call's method + path matches an operation in `openapi.json` and reports uncovered operations. Then a production build, plus a browser smoke test against a mock server that serves the documented response shapes.

## 7. Open questions ([ASSUMPTION REQUIRED])

```text
[ASSUMPTION REQUIRED] Q1 – Public endpoints
Issue: The global bearerAuth requirement covers every operation, including login, register, forgot/reset password, demo-login and refresh.
Why it matters: Taken literally, nobody could log in.
Available information: The spec has no per-operation `security: []` override (a common springdoc omission).
Handling: The Authorization header is sent only when a token is held, so these calls go out unauthenticated before login.
Required clarification: Confirm which endpoints are permitAll in the Spring Security config.
```

```text
[ASSUMPTION REQUIRED] Q2 – Error response structure
Issue: No operation documents a 4xx/5xx response or an error schema.
Why it matters: Field-level backend validation errors can't be mapped onto form fields reliably.
Available information: None in the spec.
Handling: errors.js reads `message` (or `error`/`detail`/`title`) from the body when present and falls back to a per-status message. If the body has `fieldErrors` (object) or `errors` (array of {field, message}), those are shown under the matching inputs. No invented structure is required for the UI to work.
Required clarification: The exact error DTO produced by the backend's @ControllerAdvice.
```

```text
[ASSUMPTION REQUIRED] Q3 – File upload media type
Issue: POST /api/imports/upload documents its body as application/json with `file: string(binary)`.
Why it matters: A binary file can't be sent as JSON. This is the usual springdoc rendering of a
@RequestParam MultipartFile whose `consumes` wasn't declared.
Handling: Sent as multipart/form-data with a part named `file` (the documented field name) and `plantId` as a query param.
Required clarification: Confirm the endpoint consumes multipart/form-data, and which file types and size limits are accepted.
```

```text
[ASSUMPTION REQUIRED] Q4 – Enumerated values
Issue: role, criticality and every status, severity, type, source and sender field are plain strings with no enum.
Why it matters: Selects and colored badges need the allowed values.
Available information: Roles come from GET /api/users/roles (RoleDefinitionResponse.name). Nothing for the others.
Handling: Role uses the roles endpoint. Criticality is a free-text input (with suggestions from values already seen on the machines list) and a free-text filter. Statuses and severities are shown verbatim in neutral badges; there's no color mapping for unknown values.
Required clarification: Allowed values for criticality, and whether /api/config/dictionary is meant to supply them (its shape is map<string, map<string, string[]>> with no description).
```

```text
[ASSUMPTION REQUIRED] Q5 – Role-based access
Issue: The spec doesn't say which roles may call which endpoints (platform-admin, users and audit look admin-only).
Why it matters: Navigation could show pages that return 403.
Handling: All navigation is shown and a 403 renders a "You don't have access" state. Nothing is hidden by guessed role names.
Required clarification: Role → endpoint permission matrix.
```

```text
[ASSUMPTION REQUIRED] Q6 – Password reset link format
Issue: forgotPassword emails a token, but the spec doesn't say which frontend URL the email links to.
Handling: The page lives at /reset-password?token=<token>. It calls validateResetToken first, then resetPassword.
Required clarification: The link template the backend puts in the email.
```

```text
[ASSUMPTION REQUIRED] Q7 – Export format
Issue: GET /api/records/export returns `string` with `*/*`. The format (CSV?) and file name aren't documented.
Handling: Fetched as text and saved as `maintenance-records.csv`. If the backend sends a Content-Disposition file name, that name is used.
Required clarification: Confirm the format and content type.
```

```text
[ASSUMPTION REQUIRED] Q8 – Notification deep links
Issue: NotificationResponse has entityType and entityId, but the entityType values aren't documented.
Handling: Notifications are shown with their title, message and time; no deep links are guessed.
Required clarification: The list of entityType values.
```

```text
[ASSUMPTION REQUIRED] Q9 – Missing APIs a full UI would normally have
- No delete for machines, records, schedules or users (deactivate only).
- No record update endpoint.
- No endpoint to create or edit plants, lines, failure modes or organisations; those pages are read-only.
- No "change my password / edit my profile" endpoint for the current user.
- listParts isn't plant-scoped, while most other lists are.
- No sort parameters on any list.
These are reported, not worked around.
```

```text
[ASSUMPTION REQUIRED] Q10 – CORS
Issue: The frontend dev server (localhost:5173) calls the backend at another origin.
Handling: VITE_API_BASE_URL defaults to empty in development, so requests go to /api on the Vite server, which proxies them to VITE_PROXY_TARGET (http://localhost:8080). No CORS is needed in dev. Production builds use VITE_API_BASE_URL.
Required clarification: The production hosting model (same origin, or CORS enabled on the backend).
```

## 8. Additional findings during implementation

```text
[ASSUMPTION REQUIRED] Q11 – Conversation sender values
EntryMessageResponse.sender / AssistantMessageResponse.sender are undocumented strings. Messages whose sender is
USER/HUMAN/TECHNICIAN/ME (case-insensitive) are drawn as the user's own; everything else as the agent's.
The sender label is always shown, so nothing is hidden if the guess is wrong.
```

```text
[ASSUMPTION REQUIRED] Q12 – PUT semantics (partial vs full update)
UpdateMachineRequest / UpdateScheduleRequest / UpdateUserRequest have no required fields, which suggests partial
updates. Empty optional inputs are omitted from the body, so an existing value (e.g. a machine's line) cannot be
cleared through the UI. Exception: UpdateUserRequest.plantIds is always sent ([] clears it).
Required clarification: does null / omission mean "leave unchanged" or "clear"?
```

```text
[ASSUMPTION REQUIRED] Q13 – KPI direction, import completion
- KpiValue.changePct is shown as ▲/▼ with no good/bad coloring, because whether an increase is good depends
  on the KPI key, which isn't documented.
- ImportJobResponse.status values are undocumented, so the job page polls every 3 s until finishedAt or
  errorMessage is set.
```

## 9. Verification results (Phase 4)

- `npm run verify:api`: 87 of 88 operations called, with matching method, path and query parameters, and every service function is used by a page. The one uncalled operation is the webhook `inbound`, which is intentional.
- End-to-end browser run (Playwright, production build) against `scripts/contract-mock-server.mjs`: 64 of 64 steps passed. The run covers every route, every form's validation and submit, CRUD actions, CSV export, multipart upload, refresh after an expired token, plant switching, mobile layout at 390px with no horizontal scroll, dark mode, logout, register, and forgot/reset password.
  - The mock recorded **0 contract violations**: no undocumented field, parameter or endpoint was ever sent.
  - 87 of 88 operations were exercised end-to-end.
  - The browser logged 0 console or page errors.
- Not verified: behaviour against the real Spring Boot service, which isn't in this repository. The open questions above (error body, upload media type, enum values, public endpoints) should be checked against it.
