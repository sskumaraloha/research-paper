import { request } from '../../../api/client';

/** GET /api/validation/queue?plantId&page&size → ValidationQueueResponse { pendingCount, items: PageResponse } */
export const getQueue = ({ plantId, page, size }) =>
  request({ method: 'GET', url: '/api/validation/queue', params: { plantId, page, size } });

/** GET /api/validation/pending-count?plantId → CountResponse */
export const pendingCount = (plantId) =>
  request({ method: 'GET', url: '/api/validation/pending-count', params: { plantId } });

/** POST /api/validation/{itemId}/approve → ValidationDecisionResponse */
export const approve = (itemId) => request({ method: 'POST', url: `/api/validation/${itemId}/approve` });

/** POST /api/validation/{itemId}/reject — RejectRequest { reason } → ValidationDecisionResponse */
export const reject = (itemId, reason) =>
  request({ method: 'POST', url: `/api/validation/${itemId}/reject`, data: { reason } });

/** POST /api/validation/{itemId}/edit-approve — EditValidationItemRequest → ValidationDecisionResponse */
export const editAndApprove = (itemId, payload) =>
  request({ method: 'POST', url: `/api/validation/${itemId}/edit-approve`, data: payload });

/** GET /api/validation/alias-suggestions?plantId → AliasSuggestionResponse[] */
export const listAliasSuggestions = (plantId) =>
  request({ method: 'GET', url: '/api/validation/alias-suggestions', params: { plantId } });

/** POST /api/validation/alias-suggestions/{suggestionId}/map — MapAliasRequest { machineId } → AliasMappingResponse */
export const mapAlias = (suggestionId, machineId) =>
  request({ method: 'POST', url: `/api/validation/alias-suggestions/${suggestionId}/map`, data: { machineId } });

/** POST /api/validation/alias-suggestions/{suggestionId}/dismiss → AliasSuggestionResponse */
export const dismissAlias = (suggestionId) =>
  request({ method: 'POST', url: `/api/validation/alias-suggestions/${suggestionId}/dismiss` });
