import { request } from '../../../api/client';

/** GET /api/config/failure-modes → FailureModeResponse[] */
export const listFailureModes = () => request({ method: 'GET', url: '/api/config/failure-modes' });

/** GET /api/config/dictionary → map<string, map<string, string[]>> */
export const getDictionary = () => request({ method: 'GET', url: '/api/config/dictionary' });
