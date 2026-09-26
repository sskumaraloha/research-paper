import { request } from '../../../api/client';

/** GET /api/audit?plantId&action&page&size (operationId "list") → PageResponseAuditLogResponse */
export const listAuditLogs = ({ plantId, action, page, size }) =>
  request({ method: 'GET', url: '/api/audit', params: { plantId, action, page, size } });
