import { request } from '../../../api/client';

/** GET /api/machines?plantId&query&lineId&criticality&page&size → PageResponseMachineRowResponse */
export const listMachines = ({ plantId, query, lineId, criticality, page, size }) =>
  request({ method: 'GET', url: '/api/machines', params: { plantId, query, lineId, criticality, page, size } });

/** POST /api/machines — CreateMachineRequest → 201 MachineDetailResponse */
export const createMachine = (payload) => request({ method: 'POST', url: '/api/machines', data: payload });

/** GET /api/machines/{machineId} → MachineDetailResponse */
export const getMachine = (machineId) => request({ method: 'GET', url: `/api/machines/${machineId}` });

/** PUT /api/machines/{machineId} — UpdateMachineRequest → MachineDetailResponse */
export const updateMachine = (machineId, payload) =>
  request({ method: 'PUT', url: `/api/machines/${machineId}`, data: payload });

/** GET /api/machines/{machineId}/stats → MachineStatsResponse */
export const getStats = (machineId) => request({ method: 'GET', url: `/api/machines/${machineId}/stats` });

/** GET /api/machines/{machineId}/timeline?page&size → PageResponseRecordRowResponse */
export const getTimeline = (machineId, { page, size } = {}) =>
  request({ method: 'GET', url: `/api/machines/${machineId}/timeline`, params: { page, size } });

/** GET /api/machines/{machineId}/insights → InsightResponse[] */
export const getInsights = (machineId) => request({ method: 'GET', url: `/api/machines/${machineId}/insights` });

/** POST /api/machines/{machineId}/insights/recompute → InsightResponse[] */
export const recomputeInsights = (machineId) =>
  request({ method: 'POST', url: `/api/machines/${machineId}/insights/recompute` });

/** GET /api/machines/{machineId}/aliases → AliasResponse[] */
export const listAliases = (machineId) => request({ method: 'GET', url: `/api/machines/${machineId}/aliases` });

/** POST /api/machines/{machineId}/aliases — AddAliasRequest { alias } → 201 AliasResponse */
export const addAlias = (machineId, alias) =>
  request({ method: 'POST', url: `/api/machines/${machineId}/aliases`, data: { alias } });
