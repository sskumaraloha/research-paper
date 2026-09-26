import { request } from '../../../api/client';

/** GET /api/platform/overview → PlatformOverviewResponse */
export const overview = () => request({ method: 'GET', url: '/api/platform/overview' });

/** GET /api/platform/organisations → OrganisationStatsResponse[] */
export const listOrganisations = () => request({ method: 'GET', url: '/api/platform/organisations' });

/** GET /api/platform/organisations/{organisationId} → OrganisationDetailResponse */
export const getOrganisation = (organisationId) =>
  request({ method: 'GET', url: `/api/platform/organisations/${organisationId}` });
