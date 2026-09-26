import { request } from '../../../api/client';

/** POST /api/auth/login — LoginRequest { email, password } → AuthResponse */
export const login = (payload) => request({ method: 'POST', url: '/api/auth/login', data: payload });

/** POST /api/auth/register — RegistrationRequest { fullName, email, password, phoneNumber? } → 201 AuthResponse */
export const register = (payload) => request({ method: 'POST', url: '/api/auth/register', data: payload });

/** POST /api/auth/demo-login — no body → AuthResponse */
export const demoLogin = () => request({ method: 'POST', url: '/api/auth/demo-login' });

/** POST /api/auth/logout — RefreshRequest { refreshToken } → 200, no body */
export const logout = (refreshToken) => request({ method: 'POST', url: '/api/auth/logout', data: { refreshToken } });

/** GET /api/auth/me → UserProfileResponse */
export const me = () => request({ method: 'GET', url: '/api/auth/me' });

/** POST /api/auth/forgot-password — ForgotPasswordRequest { email } → SimpleMessageResponse */
export const forgotPassword = (email) => request({ method: 'POST', url: '/api/auth/forgot-password', data: { email } });

/** GET /api/auth/reset-password/validate?token → SimpleMessageResponse */
export const validateResetToken = (token) =>
  request({ method: 'GET', url: '/api/auth/reset-password/validate', params: { token } });

/** POST /api/auth/reset-password — ResetPasswordRequest { token, newPassword } → SimpleMessageResponse */
export const resetPassword = (payload) => request({ method: 'POST', url: '/api/auth/reset-password', data: payload });

// POST /api/auth/refresh is called by api/client.js when an access token expires.
