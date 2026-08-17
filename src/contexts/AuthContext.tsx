import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import i18n from '@/shared/config/i18n/i18n';
import { clearAccessToken, loginRequest, logoutRequest, refreshRequest } from '@/api/authApi';
import type { AuthRequest, AuthUser } from '@/api/authTypes';

interface AuthContextType {
  user: AuthUser | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: AuthRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const bootstrapStartedRef = useRef(false);

  const login = useCallback(async (credentials: AuthRequest) => {
    try {
      const response = await loginRequest(credentials);
      setUser(response.user);
    } catch (error) {
      // TypeError у fetch — сеть недоступна, сервер не ответил
      if (error instanceof TypeError) {
        throw new Error(i18n.t('login.serverUnavailable'));
      }
      const message = error instanceof Error ? error.message : '';
      throw new Error(message || i18n.t('login.error'));
    }
  }, []);

  const logout = useCallback(async () => {
    // Запрос уходит с ещё живым access-токеном, состояние чистим сразу
    const request = logoutRequest().catch(() => undefined);
    clearAccessToken();
    setUser(null);
    await request;
  }, []);

  // Bootstrap: если refresh-cookie жива, входим без формы логина
  useEffect(() => {
    if (bootstrapStartedRef.current) {
      return;
    }
    bootstrapStartedRef.current = true;

    const bootstrap = async () => {
      try {
        const refreshed = await refreshRequest();
        setUser(refreshed.user);
      } catch {
        clearAccessToken();
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    void bootstrap();
  }, []);

  const value = useMemo<AuthContextType>(() => ({
    user,
    username: user?.username ?? null,
    role: user?.role ?? null,
    isAuthenticated: Boolean(user),
    isLoading,
    login,
    logout,
  }), [user, isLoading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
