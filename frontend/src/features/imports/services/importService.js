import { request } from '../../../api/client';

/**
 * POST /api/imports/upload?plantId — body { file: binary } → 201 ImportSummaryResponse
 * The spec lists application/json for a binary field; sent as multipart/form-data (see API_ANALYSIS Q3).
 */
export const uploadFile = (plantId, file, onUploadProgress) => {
  const form = new FormData();
  form.append('file', file);
  return request({
    method: 'POST',
    url: '/api/imports/upload',
    params: { plantId },
    data: form,
    timeout: 0,
    onUploadProgress,
  });
};

/** GET /api/imports/latest?plantId → ImportJobResponse */
export const getLatestJob = (plantId) => request({ method: 'GET', url: '/api/imports/latest', params: { plantId } });

/** GET /api/imports/{jobId} → ImportJobResponse */
export const getJob = (jobId) => request({ method: 'GET', url: `/api/imports/${jobId}` });

/** POST /api/imports/{jobId}/rerun → ImportJobResponse */
export const rerunJob = (jobId) => request({ method: 'POST', url: `/api/imports/${jobId}/rerun` });
