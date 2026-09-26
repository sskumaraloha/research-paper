import { request } from '../../../api/client';

/** GET /api/search?query → GlobalSearchResponse { query, machines, records, parts, failureModes } */
export const globalSearch = (query) => request({ method: 'GET', url: '/api/search', params: { query } });

/** GET /api/search/records?query → RecordMatchResponse[] */
export const searchRecords = (query) => request({ method: 'GET', url: '/api/search/records', params: { query } });

/** GET /api/search/machines?query → MachineMatch[] */
export const searchMachines = (query) => request({ method: 'GET', url: '/api/search/machines', params: { query } });
