# API Inventory

_Generated from `openapi.json` by `scripts/generate-inventory.mjs` — do not edit by hand._

Security: every operation inherits the global `bearerAuth` requirement (HTTP bearer, JWT) — no operation declares an override. Request headers: `Authorization: Bearer <accessToken>` on all calls; `X-Webhook-Token` (optional) on `inbound` only. No operation documents error responses; only success codes are listed.

## analytics-controller

### `topMachinesByDowntime` — GET `/api/analytics/top-downtime-machines`

- **query params:** `plantId` integer(int64) (required); `from` string(date); `to` string(date); `limit` integer(int32) [default 5]
- **response 200:** `MachineDowntimeResponse[]`

### `partReplacementIntervals` — GET `/api/analytics/part-replacement-intervals`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `PartIntervalResponse[]`

### `pareto` — GET `/api/analytics/pareto`

- **query params:** `plantId` integer(int64) (required); `from` string(date); `to` string(date)
- **response 200:** `ParetoBucketResponse[]`

### `lineDowntimeShare` — GET `/api/analytics/line-downtime-share`

- **query params:** `plantId` integer(int64) (required); `from` string(date); `to` string(date)
- **response 200:** `LineDowntimeShareResponse[]`

### `failureModeStats` — GET `/api/analytics/failure-mode-stats`

- **query params:** `plantId` integer(int64) (required); `from` string(date); `to` string(date)
- **response 200:** `FailureModeStatsResponse[]`

### `downtimeTrend` — GET `/api/analytics/downtime-trend`

- **query params:** `plantId` integer(int64) (required); `months` integer(int32) [default 6]
- **response 200:** `TrendResponse`

## assistant-controller

### `ask` — POST `/api/assistant/ask`

- **request body** (application/json, required): `AskRequest`
- **response 200:** `AssistantAnswerResponse`

### `suggestions` — GET `/api/assistant/suggestions`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `string[]`

### `listConversations` — GET `/api/assistant/conversations`

- **response 200:** `AssistantConversationResponse[]`

### `getConversation_1` — GET `/api/assistant/conversations/{conversationId}`

- **path params:** `conversationId` integer(int64) (required)
- **response 200:** `AssistantConversationResponse`

## audit-controller

### `list` — GET `/api/audit`

- **query params:** `plantId` integer(int64); `action` string; `page` integer(int32) [default 0]; `size` integer(int32) [default 50]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `PageResponseAuditLogResponse`

## auth-controller

### `resetPassword` — POST `/api/auth/reset-password`

- **request body** (application/json, required): `ResetPasswordRequest`
- **response 200:** `SimpleMessageResponse`

### `register` — POST `/api/auth/register`

- **request body** (application/json, required): `RegistrationRequest`
- **response 201:** `AuthResponse`

### `refresh` — POST `/api/auth/refresh`

- **request body** (application/json, required): `RefreshRequest`
- **response 200:** `AuthResponse`

### `logout` — POST `/api/auth/logout`

- **request body** (application/json, required): `RefreshRequest`
- **response 200:** _no body_

### `login` — POST `/api/auth/login`

- **request body** (application/json, required): `LoginRequest`
- **response 200:** `AuthResponse`

### `forgotPassword` — POST `/api/auth/forgot-password`

- **request body** (application/json, required): `ForgotPasswordRequest`
- **response 200:** `SimpleMessageResponse`

### `demoLogin` — POST `/api/auth/demo-login`

- **response 200:** `AuthResponse`

### `validateResetToken` — GET `/api/auth/reset-password/validate`

- **query params:** `token` string (required)
- **response 200:** `SimpleMessageResponse`

### `me` — GET `/api/auth/me`

- **response 200:** `UserProfileResponse`

## config-controller

### `listFailureModes` — GET `/api/config/failure-modes`

- **response 200:** `FailureModeResponse[]`

### `getDictionary` — GET `/api/config/dictionary`

- **response 200:** `map<string, map<string, string[]>>`

## dashboard-controller

### `plantKpis` — GET `/api/dashboard/kpis`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `PlantKpiResponse`

## entry-agent-controller

### `startConversation` — POST `/api/entry/conversations`

- **request body** (application/json, required): `StartConversationRequest`
- **response 201:** `EntryConversationResponse`

### `requestEdit` — POST `/api/entry/conversations/{conversationId}/request-edit`

- **path params:** `conversationId` integer(int64) (required)
- **response 200:** `EntryConversationResponse`

### `sendMessage` — POST `/api/entry/conversations/{conversationId}/messages`

- **path params:** `conversationId` integer(int64) (required)
- **request body** (application/json, required): `EntryMessageRequest`
- **response 200:** `EntryConversationResponse`

### `confirm` — POST `/api/entry/conversations/{conversationId}/confirm`

- **path params:** `conversationId` integer(int64) (required)
- **response 200:** `EntryConversationResponse`

### `getConversation` — GET `/api/entry/conversations/{conversationId}`

- **path params:** `conversationId` integer(int64) (required)
- **response 200:** `EntryConversationResponse`

## import-controller

### `rerunJob` — POST `/api/imports/{jobId}/rerun`

- **path params:** `jobId` integer(int64) (required)
- **response 200:** `ImportJobResponse`

### `uploadFile` — POST `/api/imports/upload`

- **query params:** `plantId` integer(int64) (required)
- **request body** (application/json): `object` { file }
- **response 201:** `ImportSummaryResponse`

### `getJob` — GET `/api/imports/{jobId}`

- **path params:** `jobId` integer(int64) (required)
- **response 200:** `ImportJobResponse`

### `getLatestJob` — GET `/api/imports/latest`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `ImportJobResponse`

## insight-controller

### `recomputeAll` — POST `/api/insights/recompute`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `InsightResponse[]`

### `listInsights` — GET `/api/insights`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `InsightResponse[]`

### `insightCount` — GET `/api/insights/count`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `CountResponse`

## machine-controller

### `getMachine` — GET `/api/machines/{machineId}`

- **path params:** `machineId` integer(int64) (required)
- **response 200:** `MachineDetailResponse`

### `updateMachine` — PUT `/api/machines/{machineId}`

- **path params:** `machineId` integer(int64) (required)
- **request body** (application/json, required): `UpdateMachineRequest`
- **response 200:** `MachineDetailResponse`

### `listMachines` — GET `/api/machines`

- **query params:** `plantId` integer(int64) (required); `query` string; `lineId` integer(int64); `criticality` string; `page` integer(int32) [default 0]; `size` integer(int32) [default 20]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `PageResponseMachineRowResponse`

### `createMachine` — POST `/api/machines`

- **request body** (application/json, required): `CreateMachineRequest`
- **response 201:** `MachineDetailResponse`

### `recomputeInsights` — POST `/api/machines/{machineId}/insights/recompute`

- **path params:** `machineId` integer(int64) (required)
- **response 200:** `InsightResponse[]`

### `listAliases` — GET `/api/machines/{machineId}/aliases`

- **path params:** `machineId` integer(int64) (required)
- **response 200:** `AliasResponse[]`

### `addAlias` — POST `/api/machines/{machineId}/aliases`

- **path params:** `machineId` integer(int64) (required)
- **request body** (application/json, required): `AddAliasRequest`
- **response 201:** `AliasResponse`

### `getTimeline` — GET `/api/machines/{machineId}/timeline`

- **path params:** `machineId` integer(int64) (required)
- **query params:** `page` integer(int32) [default 0]; `size` integer(int32) [default 20]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `PageResponseRecordRowResponse`

### `getStats` — GET `/api/machines/{machineId}/stats`

- **path params:** `machineId` integer(int64) (required)
- **response 200:** `MachineStatsResponse`

### `getInsights` — GET `/api/machines/{machineId}/insights`

- **path params:** `machineId` integer(int64) (required)
- **response 200:** `InsightResponse[]`

## maintenance-record-controller

### `listRecords` — GET `/api/records`

- **query params:** `plantId` integer(int64) (required); `machineId` integer(int64); `lineId` integer(int64); `failureModeId` integer(int64); `from` string(date); `to` string(date); `text` string; `page` integer(int32) [default 0]; `size` integer(int32) [default 20]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `PageResponseRecordRowResponse`

### `createRecord` — POST `/api/records`

- **request body** (application/json, required): `CreateRecordRequest`
- **response 201:** `RecordDetailResponse`

### `rejectRecord` — POST `/api/records/{recordId}/reject`

- **path params:** `recordId` integer(int64) (required)
- **request body** (application/json, required): `RejectRequest`
- **response 200:** `RecordDetailResponse`

### `getRecord` — GET `/api/records/{recordId}`

- **path params:** `recordId` integer(int64) (required)
- **response 200:** `RecordDetailResponse`

### `listSourceDocuments` — GET `/api/records/source-documents`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `SourceDocumentResponse[]`

### `listFilterOptions` — GET `/api/records/filter-options`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `RecordFilterOptionsResponse`

### `exportRecords` — GET `/api/records/export`

- **query params:** `plantId` integer(int64) (required); `machineId` integer(int64); `lineId` integer(int64); `failureModeId` integer(int64); `from` string(date); `to` string(date); `text` string
- **response 200:** `string`

## notification-controller

### `markRead` — POST `/api/notifications/{notificationId}/read`

- **path params:** `notificationId` integer(int64) (required)
- **response 200:** `NotificationResponse`

### `markAllRead` — POST `/api/notifications/read-all`

- **response 200:** `CountResponse`

### `listForUser` — GET `/api/notifications`

- **query params:** `page` integer(int32) [default 0]; `size` integer(int32) [default 20]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `PageResponseNotificationResponse`

### `unreadCount` — GET `/api/notifications/unread-count`

- **response 200:** `CountResponse`

### `badgeCounts` — GET `/api/notifications/badge-counts`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `BadgeCountsResponse`

## plant-controller

### `getSettings` — GET `/api/plants/{plantId}/settings`

- **path params:** `plantId` integer(int64) (required)
- **response 200:** `PlantSettingsResponse`

### `updateSettings` — PUT `/api/plants/{plantId}/settings`

- **path params:** `plantId` integer(int64) (required)
- **request body** (application/json, required): `UpdatePlantSettingsRequest`
- **response 200:** `PlantSettingsResponse`

### `listPlants` — GET `/api/plants`

- **response 200:** `PlantSummaryResponse[]`

### `listLines` — GET `/api/plants/{plantId}/lines`

- **path params:** `plantId` integer(int64) (required)
- **response 200:** `LineResponse[]`

## platform-admin-controller

### `overview` — GET `/api/platform/overview`

- **response 200:** `PlatformOverviewResponse`

### `listOrganisations` — GET `/api/platform/organisations`

- **response 200:** `OrganisationStatsResponse[]`

### `getOrganisation` — GET `/api/platform/organisations/{organisationId}`

- **path params:** `organisationId` integer(int64) (required)
- **response 200:** `OrganisationDetailResponse`

## schedule-controller

### `updateSchedule` — PUT `/api/schedules/{scheduleId}`

- **path params:** `scheduleId` integer(int64) (required)
- **request body** (application/json, required): `UpdateScheduleRequest`
- **response 200:** `ScheduleResponse`

### `listSchedules` — GET `/api/schedules`

- **query params:** `plantId` integer(int64) (required); `includeInactive` boolean [default false]
- **response 200:** `ScheduleResponse[]`

### `createSchedule` — POST `/api/schedules`

- **request body** (application/json, required): `CreateScheduleRequest`
- **response 201:** `ScheduleResponse`

### `completeSchedule` — POST `/api/schedules/{scheduleId}/complete`

- **path params:** `scheduleId` integer(int64) (required)
- **request body** (application/json, required): `CompleteScheduleRequest`
- **response 200:** `ScheduleResponse`

### `listDue` — GET `/api/schedules/due`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `ScheduleResponse[]`

## search-controller

### `globalSearch` — GET `/api/search`

- **query params:** `query` string (required)
- **response 200:** `GlobalSearchResponse`

### `searchRecords` — GET `/api/search/records`

- **query params:** `query` string (required)
- **response 200:** `RecordMatchResponse[]`

### `searchMachines` — GET `/api/search/machines`

- **query params:** `query` string (required)
- **response 200:** `MachineMatch[]`

## spare-part-controller

### `listParts` — GET `/api/parts`

- **query params:** `query` string; `page` integer(int32) [default 0]; `size` integer(int32) [default 20]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `PageResponsePartRowResponse`

### `getPartDetail` — GET `/api/parts/{partId}`

- **path params:** `partId` integer(int64) (required)
- **response 200:** `PartDetailResponse`

## user-controller

### `updateUser` — PUT `/api/users/{userId}`

- **path params:** `userId` integer(int64) (required)
- **request body** (application/json, required): `UpdateUserRequest`
- **response 200:** `UserSummaryResponse`

### `listUsers` — GET `/api/users`

- **response 200:** `UserSummaryResponse[]`

### `createUser` — POST `/api/users`

- **request body** (application/json, required): `CreateUserRequest`
- **response 201:** `UserSummaryResponse`

### `deactivateUser` — POST `/api/users/{userId}/deactivate`

- **path params:** `userId` integer(int64) (required)
- **response 200:** `UserSummaryResponse`

### `listRoles` — GET `/api/users/roles`

- **response 200:** `RoleDefinitionResponse[]`

## validation-controller

### `reject` — POST `/api/validation/{itemId}/reject`

- **path params:** `itemId` integer(int64) (required)
- **request body** (application/json, required): `RejectRequest`
- **response 200:** `ValidationDecisionResponse`

### `editAndApprove` — POST `/api/validation/{itemId}/edit-approve`

- **path params:** `itemId` integer(int64) (required)
- **request body** (application/json, required): `EditValidationItemRequest`
- **response 200:** `ValidationDecisionResponse`

### `approve` — POST `/api/validation/{itemId}/approve`

- **path params:** `itemId` integer(int64) (required)
- **response 200:** `ValidationDecisionResponse`

### `mapAlias` — POST `/api/validation/alias-suggestions/{suggestionId}/map`

- **path params:** `suggestionId` integer(int64) (required)
- **request body** (application/json, required): `MapAliasRequest`
- **response 200:** `AliasMappingResponse`

### `dismissAlias` — POST `/api/validation/alias-suggestions/{suggestionId}/dismiss`

- **path params:** `suggestionId` integer(int64) (required)
- **response 200:** `AliasSuggestionResponse`

### `getQueue` — GET `/api/validation/queue`

- **query params:** `plantId` integer(int64) (required); `page` integer(int32) [default 0]; `size` integer(int32) [default 20]
- **pagination:** `page`/`size` query params (0-based page)
- **response 200:** `ValidationQueueResponse`

### `pendingCount` — GET `/api/validation/pending-count`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `CountResponse`

### `listAliasSuggestions` — GET `/api/validation/alias-suggestions`

- **query params:** `plantId` integer(int64) (required)
- **response 200:** `AliasSuggestionResponse[]`

## whats-app-webhook-controller

### `inbound` — POST `/api/webhooks/whatsapp`

- **header params:** `X-Webhook-Token` string
- **request body** (application/json, required): `WhatsAppInboundRequest`
- **response 200:** `WhatsAppReplyResponse`

## Schemas

### AddAliasRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `alias` | string | yes | max 150 |

### AliasMappingResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `suggestionId` | integer(int64) |  |  |
| `machineId` | integer(int64) |  |  |
| `alias` | string |  |  |
| `revalidatedItemCount` | integer(int32) |  |  |

### AliasResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `alias` | string |  |  |
| `source` | string |  |  |

### AliasSuggestionResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `rawText` | string |  |  |
| `occurrences` | integer(int32) |  |  |
| `suggestedMachineId` | integer(int64) |  |  |
| `suggestedMachineName` | string |  |  |
| `confidence` | number(double) |  |  |
| `status` | string |  |  |

### AnswerSection

| Field | Type | Required | Constraints |
|---|---|---|---|
| `heading` | string |  |  |
| `text` | string |  |  |

### AskRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) | yes |  |
| `question` | string | yes | max 500 |
| `conversationId` | integer(int64) |  |  |

### AssistantAnswerResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `intent` | string |  |  |
| `title` | string |  |  |
| `tiles` | StatTile[] |  |  |
| `sections` | AnswerSection[] |  |  |
| `records` | RecordRowResponse[] |  |  |
| `followUps` | string[] |  |  |
| `conversationId` | integer(int64) |  |  |

### AssistantConversationResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `plantId` | integer(int64) |  |  |
| `title` | string |  |  |
| `updatedAt` | string(date-time) |  |  |
| `messages` | AssistantMessageResponse[] |  |  |

### AssistantMessageResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `sender` | string |  |  |
| `content` | string |  |  |
| `intent` | string |  |  |
| `createdAt` | string(date-time) |  |  |

### AuditLogResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `actorId` | integer(int64) |  |  |
| `actorName` | string |  |  |
| `action` | string |  |  |
| `entityType` | string |  |  |
| `entityId` | integer(int64) |  |  |
| `plantId` | integer(int64) |  |  |
| `detail` | string |  |  |
| `occurredAt` | string(date-time) |  |  |

### AuthResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `accessToken` | string |  |  |
| `refreshToken` | string |  |  |
| `expiresInSeconds` | integer(int64) |  |  |
| `user` | UserProfileResponse |  |  |

### BadgeCountsResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `pendingValidation` | integer(int64) |  |  |
| `unreadNotifications` | integer(int64) |  |  |
| `insights` | integer(int64) |  |  |

### CompleteScheduleRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `performedOn` | string(date) |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `notes` | string |  | max 2000 |
| `technician` | string |  | max 100 |

### CountResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `count` | integer(int64) |  |  |

### CreateMachineRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) | yes |  |
| `lineId` | integer(int64) |  |  |
| `code` | string | yes | max 30 |
| `name` | string | yes | max 150 |
| `manufacturer` | string |  | max 100 |
| `model` | string |  | max 100 |
| `criticality` | string |  |  |
| `commissionedOn` | string(date) |  |  |

### CreateRecordRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) | yes |  |
| `machineId` | integer(int64) | yes |  |
| `recordDate` | string(date) | yes |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `description` | string | yes | max 2000 |
| `actionTaken` | string |  | max 2000 |
| `technician` | string |  | max 100 |
| `failureModeId` | integer(int64) |  |  |
| `failureModeText` | string |  | max 200 |
| `partNames` | string[] |  |  |

### CreateScheduleRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machineId` | integer(int64) | yes |  |
| `title` | string | yes | max 150 |
| `description` | string |  | max 1000 |
| `intervalDays` | integer(int32) |  | ≥ 1, ≤ 3650 |
| `firstDueOn` | string(date) |  |  |

### CreateUserRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `fullName` | string | yes | max 100 |
| `email` | string | yes | max 150 |
| `password` | string | yes | min 8, max 100 |
| `role` | string | yes |  |
| `phoneNumber` | string |  | pattern `\+?[0-9]{8,15}` |
| `plantIds` | integer(int64)[] |  |  |

### EditValidationItemRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machineId` | integer(int64) |  |  |
| `failureModeId` | integer(int64) |  |  |
| `recordDate` | string(date) |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `description` | string |  | max 2000 |
| `actionTaken` | string |  | max 2000 |
| `technician` | string |  | max 100 |
| `partNames` | string[] |  |  |

### EntryConversationResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `plantId` | integer(int64) |  |  |
| `status` | string |  |  |
| `draft` | EntryDraftResponse |  |  |
| `messages` | EntryMessageResponse[] |  |  |
| `resultingRecordId` | integer(int64) |  |  |

### EntryDraftResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machineId` | integer(int64) |  |  |
| `machineName` | string |  |  |
| `machineText` | string |  |  |
| `recordDate` | string(date) |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `description` | string |  |  |
| `actionTaken` | string |  |  |
| `failureModeId` | integer(int64) |  |  |
| `failureMode` | string |  |  |
| `partsText` | string |  |  |
| `missingFields` | string[] |  |  |

### EntryMessageRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `message` | string | yes | max 1000 |

### EntryMessageResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `sender` | string |  |  |
| `content` | string |  |  |
| `createdAt` | string(date-time) |  |  |

### Evidence

| Field | Type | Required | Constraints |
|---|---|---|---|
| `recordId` | integer(int64) |  |  |
| `recordDate` | string(date) |  |  |
| `description` | string |  |  |
| `note` | string |  |  |

### FailureModeMatch

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |

### FailureModeResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `category` | string |  |  |
| `keywords` | string[] |  |  |

### FailureModeStatsResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `failureModeId` | integer(int64) |  |  |
| `name` | string |  |  |
| `category` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `totalDowntimeMinutes` | integer(int64) |  |  |
| `avgDowntimeMinutes` | number(double) |  |  |
| `machinesAffected` | integer(int64) |  |  |

### ForgotPasswordRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `email` | string | yes |  |

### GlobalSearchResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `query` | string |  |  |
| `machines` | MachineMatch[] |  |  |
| `records` | RecordMatchResponse[] |  |  |
| `parts` | PartMatch[] |  |  |
| `failureModes` | FailureModeMatch[] |  |  |

### ImportJobResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `plantId` | integer(int64) |  |  |
| `sourceDocumentId` | integer(int64) |  |  |
| `filename` | string |  |  |
| `status` | string |  |  |
| `totalRows` | integer(int32) |  |  |
| `autoImportedCount` | integer(int32) |  |  |
| `needsValidationCount` | integer(int32) |  |  |
| `rejectedCount` | integer(int32) |  |  |
| `invalidCount` | integer(int32) |  |  |
| `errorMessage` | string |  |  |
| `startedAt` | string(date-time) |  |  |
| `finishedAt` | string(date-time) |  |  |
| `steps` | ImportStepResponse[] |  |  |

### ImportStepResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string |  |  |
| `status` | string |  |  |
| `processedCount` | integer(int32) |  |  |
| `message` | string |  |  |
| `startedAt` | string(date-time) |  |  |
| `finishedAt` | string(date-time) |  |  |

### ImportSummaryResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `jobId` | integer(int64) |  |  |
| `status` | string |  |  |
| `totalRows` | integer(int32) |  |  |
| `autoImportedCount` | integer(int32) |  |  |
| `needsValidationCount` | integer(int32) |  |  |
| `rejectedCount` | integer(int32) |  |  |
| `invalidCount` | integer(int32) |  |  |

### InsightResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `type` | string |  |  |
| `severity` | string |  |  |
| `title` | string |  |  |
| `detail` | string |  |  |
| `machineId` | integer(int64) |  |  |
| `machineName` | string |  |  |
| `metricValue` | number(double) |  |  |
| `windowDays` | integer(int32) |  |  |
| `computedAt` | string(date-time) |  |  |
| `evidence` | Evidence[] |  |  |

### KpiValue

| Field | Type | Required | Constraints |
|---|---|---|---|
| `key` | string |  |  |
| `label` | string |  |  |
| `value` | number(double) |  |  |
| `previousValue` | number(double) |  |  |
| `changePct` | number(double) |  |  |

### LineDowntimeShareResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `lineId` | integer(int64) |  |  |
| `lineName` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `downtimeMinutes` | integer(int64) |  |  |
| `sharePct` | number(double) |  |  |

### LineResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |

### LoginRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `email` | string | yes |  |
| `password` | string | yes |  |

### MachineDetailResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `plantId` | integer(int64) |  |  |
| `plantName` | string |  |  |
| `lineId` | integer(int64) |  |  |
| `lineName` | string |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `manufacturer` | string |  |  |
| `model` | string |  |  |
| `criticality` | string |  |  |
| `commissionedOn` | string(date) |  |  |
| `active` | boolean |  |  |
| `status` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `totalDowntimeMinutes` | integer(int64) |  |  |
| `lastMaintenanceDate` | string(date) |  |  |
| `aliases` | AliasResponse[] |  |  |

### MachineDowntimeResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machineId` | integer(int64) |  |  |
| `machineCode` | string |  |  |
| `machineName` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `downtimeMinutes` | integer(int64) |  |  |

### MachineMatch

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `plantName` | string |  |  |

### MachineRowResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `lineName` | string |  |  |
| `criticality` | string |  |  |
| `status` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `totalDowntimeMinutes` | integer(int64) |  |  |
| `lastMaintenanceDate` | string(date) |  |  |

### MachineStatsResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machineId` | integer(int64) |  |  |
| `machineCode` | string |  |  |
| `machineName` | string |  |  |
| `status` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `totalDowntimeMinutes` | integer(int64) |  |  |
| `avgDowntimeMinutes` | number(double) |  |  |
| `mtbfDays` | number(double) |  |  |
| `lastMaintenanceDate` | string(date) |  |  |
| `topFailureModes` | FailureModeStatsResponse[] |  |  |

### MapAliasRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machineId` | integer(int64) | yes |  |

### NamedRef

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `name` | string |  |  |

### NotificationResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `type` | string |  |  |
| `title` | string |  |  |
| `message` | string |  |  |
| `entityType` | string |  |  |
| `entityId` | integer(int64) |  |  |
| `read` | boolean |  |  |
| `createdAt` | string(date-time) |  |  |

### OrganisationDetailResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `plants` | PlantStats[] |  |  |
| `users` | UserSummaryResponse[] |  |  |

### OrganisationStatsResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `plantCount` | integer(int64) |  |  |
| `userCount` | integer(int64) |  |  |
| `machineCount` | integer(int64) |  |  |
| `recordCount` | integer(int64) |  |  |
| `importJobCount` | integer(int64) |  |  |
| `lastActivityAt` | string(date-time) |  |  |

### PageResponseAuditLogResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | AuditLogResponse[] |  |  |
| `page` | integer(int32) |  |  |
| `size` | integer(int32) |  |  |
| `totalElements` | integer(int64) |  |  |
| `totalPages` | integer(int32) |  |  |
| `first` | boolean |  |  |
| `last` | boolean |  |  |

### PageResponseMachineRowResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | MachineRowResponse[] |  |  |
| `page` | integer(int32) |  |  |
| `size` | integer(int32) |  |  |
| `totalElements` | integer(int64) |  |  |
| `totalPages` | integer(int32) |  |  |
| `first` | boolean |  |  |
| `last` | boolean |  |  |

### PageResponseNotificationResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | NotificationResponse[] |  |  |
| `page` | integer(int32) |  |  |
| `size` | integer(int32) |  |  |
| `totalElements` | integer(int64) |  |  |
| `totalPages` | integer(int32) |  |  |
| `first` | boolean |  |  |
| `last` | boolean |  |  |

### PageResponsePartRowResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | PartRowResponse[] |  |  |
| `page` | integer(int32) |  |  |
| `size` | integer(int32) |  |  |
| `totalElements` | integer(int64) |  |  |
| `totalPages` | integer(int32) |  |  |
| `first` | boolean |  |  |
| `last` | boolean |  |  |

### PageResponseRecordRowResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | RecordRowResponse[] |  |  |
| `page` | integer(int32) |  |  |
| `size` | integer(int32) |  |  |
| `totalElements` | integer(int64) |  |  |
| `totalPages` | integer(int32) |  |  |
| `first` | boolean |  |  |
| `last` | boolean |  |  |

### PageResponseValidationItemResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | ValidationItemResponse[] |  |  |
| `page` | integer(int32) |  |  |
| `size` | integer(int32) |  |  |
| `totalElements` | integer(int64) |  |  |
| `totalPages` | integer(int32) |  |  |
| `first` | boolean |  |  |
| `last` | boolean |  |  |

### ParetoBucketResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `failureModeId` | integer(int64) |  |  |
| `failureMode` | string |  |  |
| `category` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `downtimeMinutes` | integer(int64) |  |  |
| `downtimeSharePct` | number(double) |  |  |
| `cumulativeSharePct` | number(double) |  |  |

### PartDetailResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `partNumber` | string |  |  |
| `name` | string |  |  |
| `category` | string |  |  |
| `usageCount` | integer(int64) |  |  |
| `lastUsedDate` | string(date) |  |  |
| `avgReplacementIntervalDays` | number(double) |  |  |
| `machinesUsedOn` | NamedRef[] |  |  |
| `recentRecords` | RecordRowResponse[] |  |  |

### PartIntervalResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `partId` | integer(int64) |  |  |
| `partName` | string |  |  |
| `usageCount` | integer(int64) |  |  |
| `avgIntervalDays` | number(double) |  |  |

### PartMatch

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `partNumber` | string |  |  |
| `name` | string |  |  |

### PartRowResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `partNumber` | string |  |  |
| `name` | string |  |  |
| `category` | string |  |  |
| `usageCount` | integer(int64) |  |  |
| `lastUsedDate` | string(date) |  |  |

### PlantKpiResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) |  |  |
| `windowFrom` | string(date) |  |  |
| `windowTo` | string(date) |  |  |
| `kpis` | KpiValue[] |  |  |

### PlantSettingsResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) |  |  |
| `plantName` | string |  |  |
| `autoApproveThreshold` | number(double) |  |  |
| `lowConfidenceThreshold` | number(double) |  |  |
| `downtimeAlertMinutes` | integer(int32) |  |  |

### PlantStats

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `machineCount` | integer(int64) |  |  |
| `recordCount` | integer(int64) |  |  |
| `downtimeLast30DaysMinutes` | integer(int64) |  |  |
| `pendingValidations` | integer(int64) |  |  |

### PlantSummaryResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `code` | string |  |  |
| `name` | string |  |  |
| `location` | string |  |  |
| `organisationName` | string |  |  |

### PlatformOverviewResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `organisations` | integer(int64) |  |  |
| `plants` | integer(int64) |  |  |
| `users` | integer(int64) |  |  |
| `machines` | integer(int64) |  |  |
| `maintenanceRecords` | integer(int64) |  |  |
| `recordsLast30Days` | integer(int64) |  |  |
| `importJobs` | integer(int64) |  |  |
| `pendingValidations` | integer(int64) |  |  |

### RecordDetailResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `plantId` | integer(int64) |  |  |
| `recordDate` | string(date) |  |  |
| `machineId` | integer(int64) |  |  |
| `machineCode` | string |  |  |
| `machineName` | string |  |  |
| `lineName` | string |  |  |
| `failureModeId` | integer(int64) |  |  |
| `failureMode` | string |  |  |
| `failureModeCategory` | string |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `description` | string |  |  |
| `actionTaken` | string |  |  |
| `technician` | string |  |  |
| `source` | string |  |  |
| `status` | string |  |  |
| `confidence` | number(double) |  |  |
| `rejectedReason` | string |  |  |
| `spareParts` | NamedRef[] |  |  |
| `sourceDocumentId` | integer(int64) |  |  |
| `sourceDocumentName` | string |  |  |
| `createdByName` | string |  |  |

### RecordFilterOptionsResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `machines` | NamedRef[] |  |  |
| `lines` | NamedRef[] |  |  |
| `failureModes` | NamedRef[] |  |  |

### RecordMatchResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `recordDate` | string(date) |  |  |
| `machineName` | string |  |  |
| `failureMode` | string |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `snippet` | string |  |  |

### RecordRowResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `recordDate` | string(date) |  |  |
| `machineId` | integer(int64) |  |  |
| `machineCode` | string |  |  |
| `machineName` | string |  |  |
| `failureMode` | string |  |  |
| `failureModeCategory` | string |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `technician` | string |  |  |
| `source` | string |  |  |
| `status` | string |  |  |
| `confidence` | number(double) |  |  |
| `description` | string |  |  |

### RefreshRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `refreshToken` | string | yes |  |

### RegistrationRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `fullName` | string | yes | max 100 |
| `email` | string | yes | max 150 |
| `password` | string | yes | min 8, max 100 |
| `phoneNumber` | string |  | pattern `\+?[0-9]{8,15}` |

### RejectRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `reason` | string | yes | max 500 |

### ResetPasswordRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `token` | string | yes |  |
| `newPassword` | string | yes | min 8, max 100 |

### RoleDefinitionResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string |  |  |
| `description` | string |  |  |

### ScheduleResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `machineId` | integer(int64) |  |  |
| `machineName` | string |  |  |
| `title` | string |  |  |
| `description` | string |  |  |
| `intervalDays` | integer(int32) |  |  |
| `lastPerformedOn` | string(date) |  |  |
| `nextDueOn` | string(date) |  |  |
| `daysUntilDue` | integer(int64) |  |  |
| `status` | string |  |  |
| `active` | boolean |  |  |

### SimpleMessageResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `message` | string |  |  |

### SourceDocumentResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `filename` | string |  |  |
| `contentType` | string |  |  |
| `sizeBytes` | integer(int64) |  |  |
| `uploadedByName` | string |  |  |
| `uploadedAt` | string(date-time) |  |  |

### StartConversationRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) | yes |  |
| `message` | string |  | max 1000 |

### StatTile

| Field | Type | Required | Constraints |
|---|---|---|---|
| `label` | string |  |  |
| `value` | string |  |  |
| `unit` | string |  |  |

### TrendPoint

| Field | Type | Required | Constraints |
|---|---|---|---|
| `yearMonth` | string |  |  |
| `recordCount` | integer(int64) |  |  |
| `downtimeMinutes` | integer(int64) |  |  |

### TrendResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `plantId` | integer(int64) |  |  |
| `points` | TrendPoint[] |  |  |

### UpdateMachineRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string |  | max 150 |
| `lineId` | integer(int64) |  |  |
| `manufacturer` | string |  | max 100 |
| `model` | string |  | max 100 |
| `criticality` | string |  |  |
| `commissionedOn` | string(date) |  |  |
| `active` | boolean |  |  |

### UpdatePlantSettingsRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `autoApproveThreshold` | number(double) | yes | ≥ 0, ≤ 1 |
| `lowConfidenceThreshold` | number(double) | yes | ≥ 0, ≤ 1 |
| `downtimeAlertMinutes` | integer(int32) | yes |  |

### UpdateScheduleRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `title` | string |  | max 150 |
| `description` | string |  | max 1000 |
| `intervalDays` | integer(int32) |  | ≥ 1, ≤ 3650 |
| `nextDueOn` | string(date) |  |  |
| `active` | boolean |  |  |

### UpdateUserRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `fullName` | string |  | max 100 |
| `role` | string |  |  |
| `phoneNumber` | string |  | pattern `\+?[0-9]{8,15}` |
| `plantIds` | integer(int64)[] |  |  |
| `active` | boolean |  |  |

### UserProfileResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `fullName` | string |  |  |
| `email` | string |  |  |
| `role` | string |  |  |
| `plantIds` | integer(int64)[] |  |  |

### UserSummaryResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `fullName` | string |  |  |
| `email` | string |  |  |
| `phoneNumber` | string |  |  |
| `role` | string |  |  |
| `active` | boolean |  |  |
| `plantIds` | integer(int64)[] |  |  |

### ValidationDecisionResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `itemId` | integer(int64) |  |  |
| `status` | string |  |  |
| `recordId` | integer(int64) |  |  |

### ValidationItemResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | integer(int64) |  |  |
| `jobId` | integer(int64) |  |  |
| `sourceFilename` | string |  |  |
| `rowNumber` | integer(int32) |  |  |
| `reasons` | string[] |  |  |
| `confidence` | number(double) |  |  |
| `machineText` | string |  |  |
| `machineId` | integer(int64) |  |  |
| `machineName` | string |  |  |
| `failureModeId` | integer(int64) |  |  |
| `failureModeName` | string |  |  |
| `recordDate` | string(date) |  |  |
| `downtimeMinutes` | integer(int32) |  |  |
| `description` | string |  |  |
| `actionTaken` | string |  |  |
| `technician` | string |  |  |
| `partsText` | string |  |  |
| `rawData` | map<string, string> |  |  |

### ValidationQueueResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `pendingCount` | integer(int64) |  |  |
| `items` | PageResponseValidationItemResponse |  |  |

### WhatsAppInboundRequest

| Field | Type | Required | Constraints |
|---|---|---|---|
| `from` | string | yes | max 20 |
| `text` | string | yes | max 1000 |

### WhatsAppReplyResponse

| Field | Type | Required | Constraints |
|---|---|---|---|
| `conversationId` | integer(int64) |  |  |
| `status` | string |  |  |
| `reply` | string |  |  |

