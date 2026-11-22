import type { Role, TokenResponse } from './types';

/** What the UI knows about the signed-in user. Rebuilt from the access token's claims. */
export interface Session {
  accessToken: string;
  refreshToken: string;
  /** epoch millis */
  expiresAt: number;
  username: string;
  role: Role;
  merchantId: number | null;
}

const KEY = 'settle.session';

interface Claims {
  sub?: string;
  role?: Role;
  mid?: number;
  exp?: number;
}

export function decodeJwt(token: string): Claims {
  try {
    const part = token.split('.')[1]!.replace(/-/g, '+').replace(/_/g, '/');
    const json = new TextDecoder().decode(Uint8Array.from(atob(part), (c) => c.charCodeAt(0)));
    return JSON.parse(json) as Claims;
  } catch {
    return {};
  }
}

export function sessionFromTokens(tokens: TokenResponse, previous?: Session | null, now = Date.now()): Session {
  const claims = decodeJwt(tokens.accessToken);
  return {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken ?? previous?.refreshToken ?? '',
    expiresAt: claims.exp ? claims.exp * 1000 : now + tokens.expiresIn * 1000,
    username: claims.sub ?? previous?.username ?? '',
    role: claims.role ?? previous?.role ?? 'MERCHANT',
    merchantId: claims.mid ?? null,
  };
}

function load(): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

type Listener = () => void;
let current: Session | null = load();
const listeners = new Set<Listener>();

/**
 * Tiny external store (read with useSyncExternalStore). The API client reads it on every call,
 * so a refreshed token is used immediately, with no stale closure. Kept in sessionStorage: the
 * tab survives a reload, closing it signs out. Trade-off documented in the README.
 */
export const sessionStore = {
  get: (): Session | null => current,
  set(next: Session | null) {
    current = next;
    try {
      if (next) sessionStorage.setItem(KEY, JSON.stringify(next));
      else sessionStorage.removeItem(KEY);
    } catch {
      /* storage blocked: session lives in memory only */
    }
    listeners.forEach((l) => l());
  },
  subscribe(listener: Listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
