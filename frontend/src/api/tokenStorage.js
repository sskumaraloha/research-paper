// Persists the AuthResponse tokens (accessToken, refreshToken, expiresInSeconds) between reloads.
const KEY = 'mip.auth';

export function loadTokens() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) ?? null;
  } catch {
    return null;
  }
}

export function saveTokens({ accessToken, refreshToken, expiresInSeconds }) {
  const tokens = {
    accessToken,
    refreshToken,
    expiresAt: expiresInSeconds ? Date.now() + expiresInSeconds * 1000 : null,
  };
  try {
    localStorage.setItem(KEY, JSON.stringify(tokens));
  } catch {
    // Storage unavailable (private mode): tokens live only for this page load.
  }
  memory = tokens;
  return tokens;
}

export function clearTokens() {
  memory = null;
  try {
    localStorage.removeItem(KEY);
  } catch {
    // ignore
  }
}

let memory = loadTokens();

export const getAccessToken = () => memory?.accessToken ?? null;
export const getRefreshToken = () => memory?.refreshToken ?? null;
