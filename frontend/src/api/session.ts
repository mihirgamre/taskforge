const ACCESS_TOKEN_KEY = 'taskforge.accessToken';
const REFRESH_TOKEN_KEY = 'taskforge.refreshToken';

export type StoredSession = {
  accessToken: string;
  refreshToken: string;
};

export function getStoredSession(): StoredSession | null {
  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY);
  const refreshToken = window.localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!accessToken || !refreshToken) {
    return null;
  }
  return { accessToken, refreshToken };
}

export function storeSession(session: StoredSession) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken);
}

export function clearSession() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}
