import { request } from '../../../api/client';

/** POST /api/assistant/ask — AskRequest { plantId, question, conversationId? } → AssistantAnswerResponse */
export const ask = (payload) => request({ method: 'POST', url: '/api/assistant/ask', data: payload });

/** GET /api/assistant/suggestions?plantId → string[] */
export const suggestions = (plantId) =>
  request({ method: 'GET', url: '/api/assistant/suggestions', params: { plantId } });

/** GET /api/assistant/conversations → AssistantConversationResponse[] */
export const listConversations = () => request({ method: 'GET', url: '/api/assistant/conversations' });

/** GET /api/assistant/conversations/{conversationId} (operationId getConversation_1) → AssistantConversationResponse */
export const getAssistantConversation = (conversationId) =>
  request({ method: 'GET', url: `/api/assistant/conversations/${conversationId}` });
