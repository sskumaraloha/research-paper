import { request } from '../../../api/client';

/** POST /api/entry/conversations — StartConversationRequest { plantId, message? } → 201 EntryConversationResponse */
export const startConversation = (payload) =>
  request({ method: 'POST', url: '/api/entry/conversations', data: payload });

/** GET /api/entry/conversations/{conversationId} → EntryConversationResponse */
export const getConversation = (conversationId) =>
  request({ method: 'GET', url: `/api/entry/conversations/${conversationId}` });

/** POST /api/entry/conversations/{conversationId}/messages — EntryMessageRequest { message } → EntryConversationResponse */
export const sendMessage = (conversationId, message) =>
  request({ method: 'POST', url: `/api/entry/conversations/${conversationId}/messages`, data: { message } });

/** POST /api/entry/conversations/{conversationId}/confirm → EntryConversationResponse */
export const confirm = (conversationId) =>
  request({ method: 'POST', url: `/api/entry/conversations/${conversationId}/confirm` });

/** POST /api/entry/conversations/{conversationId}/request-edit → EntryConversationResponse */
export const requestEdit = (conversationId) =>
  request({ method: 'POST', url: `/api/entry/conversations/${conversationId}/request-edit` });
