import { client, request } from '../../../api/client';

/** GET /api/records?plantId&machineId&lineId&failureModeId&from&to&text&page&size → PageResponseRecordRowResponse */
export const listRecords = ({ plantId, machineId, lineId, failureModeId, from, to, text, page, size }) =>
  request({
    method: 'GET',
    url: '/api/records',
    params: { plantId, machineId, lineId, failureModeId, from, to, text, page, size },
  });

/** POST /api/records — CreateRecordRequest → 201 RecordDetailResponse */
export const createRecord = (payload) => request({ method: 'POST', url: '/api/records', data: payload });

/** GET /api/records/{recordId} → RecordDetailResponse */
export const getRecord = (recordId) => request({ method: 'GET', url: `/api/records/${recordId}` });

/** POST /api/records/{recordId}/reject — RejectRequest { reason } → RecordDetailResponse */
export const rejectRecord = (recordId, reason) =>
  request({ method: 'POST', url: `/api/records/${recordId}/reject`, data: { reason } });

/** GET /api/records/filter-options?plantId → RecordFilterOptionsResponse { machines, lines, failureModes } */
export const listFilterOptions = (plantId) =>
  request({ method: 'GET', url: '/api/records/filter-options', params: { plantId } });

/** GET /api/records/source-documents?plantId → SourceDocumentResponse[] */
export const listSourceDocuments = (plantId) =>
  request({ method: 'GET', url: '/api/records/source-documents', params: { plantId } });

/**
 * GET /api/records/export?plantId&machineId&lineId&failureModeId&from&to&text → string
 * Returns { content, filename } so the caller can save it (format undocumented — see API_ANALYSIS Q7).
 */
export const exportRecords = async ({ plantId, machineId, lineId, failureModeId, from, to, text }) => {
  const response = await client.request({
    method: 'GET',
    url: '/api/records/export',
    params: { plantId, machineId, lineId, failureModeId, from, to, text },
    responseType: 'text',
    headers: { Accept: '*/*' },
  });
  const disposition = response.headers?.['content-disposition'] ?? '';
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  return {
    content: response.data,
    contentType: response.headers?.['content-type'] || 'text/csv',
    filename: match ? decodeURIComponent(match[1]) : 'maintenance-records.csv',
  };
};
