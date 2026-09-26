import axios from 'axios';
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from './tokenStorage';
import { toApiError } from './errors';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

export const client = axios.create({
  baseURL,
  timeout: Number(import.meta.env.VITE_API_TIMEOUT_MS) || 30000,
  headers: { Accept: 'application/json' },
  // Omit empty optional query params instead of sending "?lineId=".
  paramsSerializer: {
    serialize: (params) => {
      const search = new URLSearchParams();
      for (const [key, value] of Object.entries(params ?? {})) {
        if (value === undefined || value === null || value === '') continue;
        search.append(key, String(value));
      }
      return search.toString();
    },
  },
});

// bearerAuth (HTTP bearer, JWT) is the only security scheme in openapi.json.
client.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let onSessionExpired = () => {};
export function setSessionExpiredHandler(handler) {
  onSessionExpired = handler;
}

// Single in-flight refresh shared by concurrent 401s.
let refreshPromise = null;
function refreshTokens() {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken();
    // POST /api/auth/refresh  body: RefreshRequest { refreshToken }  → AuthResponse
    refreshPromise = (refreshToken
      ? axios.post(`${baseURL}/api/auth/refresh`, { refreshToken }, { timeout: client.defaults.timeout })
      : Promise.reject(new Error('No refresh token'))
    )
      .then(({ data }) => saveTokens(data))
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

const NO_REFRESH_PATHS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/register', '/api/auth/demo-login'];

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const skip = !original || original._retried || NO_REFRESH_PATHS.some((p) => original.url?.startsWith(p));
    if (status === 401 && !skip && getRefreshToken()) {
      original._retried = true;
      try {
        await refreshTokens();
        original.headers.Authorization = `Bearer ${getAccessToken()}`;
        return client(original);
      } catch {
        clearTokens();
        onSessionExpired();
      }
    } else if (status === 401 && !skip) {
      clearTokens();
      onSessionExpired();
    }
    return Promise.reject(toApiError(error));
  },
);

/** Returns response data; every service function goes through here. */
export async function request(config) {
  const { data } = await client.request(config);
  return data;
}
