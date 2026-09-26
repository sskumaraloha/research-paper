import { request } from '../../../api/client';

/** GET /api/insights?plantId → InsightResponse[] */
export const listInsights = (plantId) => request({ method: 'GET', url: '/api/insights', params: { plantId } });

/** GET /api/insights/count?plantId → CountResponse */
export const insightCount = (plantId) => request({ method: 'GET', url: '/api/insights/count', params: { plantId } });

/** POST /api/insights/recompute?plantId → InsightResponse[] */
export const recomputeAll = (plantId) =>
  request({ method: 'POST', url: '/api/insights/recompute', params: { plantId } });
