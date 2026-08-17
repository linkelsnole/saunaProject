import type { AuthRequest, AuthResponse, AuthUser } from './authTypes';

// Access-токен живёт только в памяти вкладки, refresh-токен — в httpOnly cookie.
// В localStorage ничего не пишем.
let accessToken: string | null = null;

export const getAccessToken = (): string | null => accessToken;

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

export const clearAccessToken = () => setAccessToken(null);

// Единственный параллельный refresh: все 401 ждут один и тот же запрос
let refreshPromise: Promise<string | null> | null = null;

const isAuthEndpoint = (url: string) =>
  url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');

const isAuthFailureStatus = (status: number) => status === 401 || status === 403;

async function parseErrorMessage(response: Response): Promise<string | null> {
  try {
    const data = await response.json();
    return data?.error ?? data?.message ?? null;
  } catch {
    return null;
  }
}

async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  return fetch(url, {
    credentials: 'include',
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  });
}

export async function loginRequest(payload: AuthRequest): Promise<AuthResponse> {
  const response = await authFetch('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await parseErrorMessage(response);
    throw new Error(message ?? '');
  }

  const data: AuthResponse = await response.json();
  setAccessToken(data.accessToken);
  return data;
}

export async function refreshRequest(): Promise<AuthResponse> {
  const response = await authFetch('/api/v1/auth/refresh', { method: 'POST' });

  if (!response.ok) {
    clearAccessToken();
    throw new Error('Refresh failed');
  }

  const data: AuthResponse = await response.json();
  setAccessToken(data.accessToken);
  return data;
}

export async function logoutRequest(): Promise<void> {
  await fetchWithAuth('/api/v1/auth/logout', { method: 'POST' });
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await fetchWithAuth('/api/v1/auth/me');
  if (!response.ok) {
    throw new Error('Failed to get current user');
  }
  return response.json();
}

const refreshAccessToken = async (): Promise<string | null> => {
  const data = await refreshRequest();
  return data.accessToken ?? null;
};

function withAuthHeader(options: RequestInit, token: string | null): RequestInit {
  return {
    credentials: 'include',
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  };
}

export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const response = await fetch(url, withAuthHeader(options, getAccessToken()));

  if (!isAuthFailureStatus(response.status) || isAuthEndpoint(url)) {
    return response;
  }

  // Access-токен истёк: пробуем один общий refresh и повторяем запрос
  try {
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
    }

    const refreshedToken = await refreshPromise;
    if (refreshedToken) {
      return fetch(url, withAuthHeader(options, refreshedToken));
    }
  } catch {
    clearAccessToken();
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }

  return response;
}
