import type { ReactElement, ReactNode } from 'react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/lib/auth';
import { I18nProvider, type Lang } from '@/lib/i18n';
import { sessionStore, type Session } from '@/lib/session';

export function newQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } } });
}

export function Providers({ children, client, lang = 'en', path = '/' }: { children: ReactNode; client?: QueryClient; lang?: Lang; path?: string }) {
  return (
    <QueryClientProvider client={client ?? newQueryClient()}>
      <I18nProvider initial={lang}>
        <AuthProvider>
          <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>
        </AuthProvider>
      </I18nProvider>
    </QueryClientProvider>
  );
}

export function renderWithProviders(ui: ReactElement, opts: { client?: QueryClient; lang?: Lang; path?: string } = {}) {
  return render(<Providers {...opts}>{ui}</Providers>);
}

/** A JWT-shaped token with the claims the UI reads (signature is irrelevant client-side). */
export function fakeJwt(claims: Record<string, unknown>): string {
  const b64 = (o: unknown) => btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${b64({ alg: 'HS256' })}.${b64(claims)}.sig`;
}

export function signIn(over: Partial<Session> = {}): Session {
  const s: Session = {
    accessToken: 'access-1', refreshToken: 'refresh-1', expiresAt: Date.now() + 15 * 60_000,
    username: 'atlas', role: 'MERCHANT', merchantId: 1, ...over,
  };
  sessionStore.set(s);
  return s;
}

export function jsonResponse(body: unknown, status = 200, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } });
}

export function problem(status: number, title: string, detail: string, errors?: Record<string, string[]>) {
  return new Response(JSON.stringify({ type: 'about:blank', title, status, detail, errors }), {
    status, headers: { 'Content-Type': 'application/problem+json' },
  });
}
