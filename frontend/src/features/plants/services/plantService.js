import { request } from '../../../api/client';

/** GET /api/plants → PlantSummaryResponse[] */
export const listPlants = () => request({ method: 'GET', url: '/api/plants' });

/** GET /api/plants/{plantId}/lines → LineResponse[] */
export const listLines = (plantId) => request({ method: 'GET', url: `/api/plants/${plantId}/lines` });

/** GET /api/plants/{plantId}/settings → PlantSettingsResponse */
export const getSettings = (plantId) => request({ method: 'GET', url: `/api/plants/${plantId}/settings` });

/** PUT /api/plants/{plantId}/settings — UpdatePlantSettingsRequest → PlantSettingsResponse */
export const updateSettings = (plantId, payload) =>
  request({ method: 'PUT', url: `/api/plants/${plantId}/settings`, data: payload });
