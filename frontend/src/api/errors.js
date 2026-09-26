// The OpenAPI contract documents no error schema (see docs/API_ANALYSIS.md, Q2), so errors are
// normalized defensively: use the backend's message/field errors when present, else a status-based message.
const STATUS_MESSAGES = {
  400: 'The request was invalid. Please check the highlighted fields.',
  401: 'Your session has expired. Please sign in again.',
  403: "You don't have permission to do this.",
  404: 'The requested item was not found.',
  409: 'This conflicts with existing data.',
  422: 'Some fields are invalid.',
  500: 'The server encountered an error. Please try again later.',
};

export class ApiError extends Error {
  constructor({ status, message, fieldErrors, data }) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.data = data;
  }
}

function extractFieldErrors(data) {
  if (!data || typeof data !== 'object') return {};
  if (data.fieldErrors && typeof data.fieldErrors === 'object' && !Array.isArray(data.fieldErrors)) {
    return data.fieldErrors;
  }
  const list = Array.isArray(data.errors) ? data.errors : Array.isArray(data.fieldErrors) ? data.fieldErrors : null;
  if (!list) return {};
  return Object.fromEntries(
    list.filter((e) => e && e.field).map((e) => [e.field, e.message ?? e.defaultMessage ?? 'Invalid value']),
  );
}

export function toApiError(error) {
  if (error instanceof ApiError) return error;
  if (error?.code === 'ECONNABORTED') {
    return new ApiError({ status: 0, message: 'The request timed out. Please try again.' });
  }
  const response = error?.response;
  if (!response) {
    return new ApiError({ status: 0, message: 'Unable to reach the server. Check your connection.' });
  }
  const { status, data } = response;
  const body = typeof data === 'string' ? null : data;
  const serverMessage = body?.message || body?.detail || body?.error || body?.title;
  return new ApiError({
    status,
    message: serverMessage || STATUS_MESSAGES[status] || `Request failed (HTTP ${status}).`,
    fieldErrors: extractFieldErrors(body),
    data: body,
  });
}
