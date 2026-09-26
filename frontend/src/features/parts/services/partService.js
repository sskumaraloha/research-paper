import { request } from '../../../api/client';

/** GET /api/parts?query&page&size → PageResponsePartRowResponse */
export const listParts = ({ query, page, size }) =>
  request({ method: 'GET', url: '/api/parts', params: { query, page, size } });

/** GET /api/parts/{partId} → PartDetailResponse */
export const getPartDetail = (partId) => request({ method: 'GET', url: `/api/parts/${partId}` });
