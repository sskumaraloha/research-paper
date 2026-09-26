import { request } from '../../../api/client';

/** GET /api/users → UserSummaryResponse[] */
export const listUsers = () => request({ method: 'GET', url: '/api/users' });

/** GET /api/users/roles → RoleDefinitionResponse[] */
export const listRoles = () => request({ method: 'GET', url: '/api/users/roles' });

/** POST /api/users — CreateUserRequest → 201 UserSummaryResponse */
export const createUser = (payload) => request({ method: 'POST', url: '/api/users', data: payload });

/** PUT /api/users/{userId} — UpdateUserRequest → UserSummaryResponse */
export const updateUser = (userId, payload) => request({ method: 'PUT', url: `/api/users/${userId}`, data: payload });

/** POST /api/users/{userId}/deactivate → UserSummaryResponse */
export const deactivateUser = (userId) => request({ method: 'POST', url: `/api/users/${userId}/deactivate` });
