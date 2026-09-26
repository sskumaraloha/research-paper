import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { setSessionExpiredHandler } from '../api/client';
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../api/tokenStorage';
import * as authService from '../features/auth/services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // user: UserProfileResponse { id, fullName, email, role, plantIds }
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState(getAccessToken() ? 'loading' : 'anonymous');
  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      setStatus('anonymous');
      setSessionExpired(true);
    });
  }, []);

  useEffect(() => {
    if (!getAccessToken()) return;
    let cancelled = false;
    authService
      .me()
      .then((profile) => {
        if (cancelled) return;
        setUser(profile);
        setStatus('authenticated');
      })
      .catch(() => {
        if (cancelled) return;
        clearTokens();
        setStatus('anonymous');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  /** Accepts an AuthResponse { accessToken, refreshToken, expiresInSeconds, user }. */
  const signIn = useCallback((authResponse) => {
    saveTokens(authResponse);
    setUser(authResponse.user);
    setStatus('authenticated');
    setSessionExpired(false);
  }, []);

  const signOut = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) await authService.logout(refreshToken);
    } catch {
      // Server-side revocation failed; local sign-out still proceeds.
    }
    clearTokens();
    setUser(null);
    setStatus('anonymous');
  }, []);

  const value = useMemo(
    () => ({ user, status, signIn, signOut, sessionExpired }),
    [user, status, signIn, signOut, sessionExpired],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
