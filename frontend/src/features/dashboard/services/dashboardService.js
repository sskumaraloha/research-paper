import { request } from '../../../api/client';

/** GET /api/dashboard/kpis?plantId → PlantKpiResponse */
export const plantKpis = (plantId) => request({ method: 'GET', url: '/api/dashboard/kpis', params: { plantId } });
