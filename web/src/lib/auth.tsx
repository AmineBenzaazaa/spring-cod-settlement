import { createContext, useCallback, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { api, onSessionExpiredCallback } from './api';
import { useI18n } from './i18n';
import { sessionFromTokens, sessionStore, type Session } from './session';

interface AuthValue {
  session: Session | null;
  isStaff: boolean;
  login: (username: string, password: string) => Promise<Session>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(sessionStore.subscribe, sessionStore.get);
  const queryClient = useQueryClient();
  const { t } = useI18n();

  // The API client calls this when the refresh token is rejected too: drop cached data, say why.
  useEffect(() => {
    onSessionExpiredCallback(() => {
      queryClient.clear();
      toast.error(t('session.expired'), { id: 'session-expired' });
    });
  }, [queryClient, t]);

  const logout = useCallback(() => {
    queryClient.clear();
    sessionStore.set(null);
  }, [queryClient]);

  const login = useCallback(async (username: string, password: string) => {
    queryClient.clear();
    const s = sessionFromTokens(await api.login(username, password));
    sessionStore.set(s);
    return s;
  }, [queryClient]);

  const value = useMemo(() => ({ session, isStaff: session?.role === 'STAFF', login, logout }), [session, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}
