import { sessionFromTokens, sessionStore } from './session';
import type {
  Courier, Dispute, DisputeDecision, DisputeStatus, Order, OrderStatus, Outcome, Overview, Page, ReconciliationReport,
  Statement, StatementUploadResponse, TokenResponse,
} from './types';

/** An RFC 7807 ProblemDetail from the API, with the field errors flattened for forms. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly title: string,
    readonly detail: string | undefined,
    readonly fieldErrors: Record<string, string[]> = {},
    readonly type?: string,
  ) {
    super(detail ?? title);
  }
}

interface ProblemBody {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string[] | string>;
}

async function toApiError(res: Response): Promise<ApiError> {
  let problem: ProblemBody = {};
  try {
    problem = (await res.json()) as ProblemBody;
  } catch {
    /* empty or non-JSON body (e.g. nginx 502 page) */
  }
  const errors: Record<string, string[]> = {};
  for (const [field, msgs] of Object.entries(problem.errors ?? {})) errors[field] = Array.isArray(msgs) ? msgs : [msgs];
  return new ApiError(res.status, problem.title ?? res.statusText ?? 'Error', problem.detail, errors, problem.type);
}

let onSessionExpired: () => void = () => {};
export function onSessionExpiredCallback(cb: () => void) {
  onSessionExpired = cb;
}

/** Refresh this long before the access token expires, instead of waiting for a 401. */
const REFRESH_SKEW_MS = 30_000;
let refreshing: Promise<string | null> | null = null;

/**
 * Single-flight refresh: when five queries hit a 401 at the same moment, exactly one
 * POST /api/auth/refresh is sent and all five wait for it, then retry with the new token.
 */
export function refreshAccessToken(): Promise<string | null> {
  refreshing ??= (async () => {
    const session = sessionStore.get();
    if (!session?.refreshToken) return null;
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      });
      if (!res.ok) return null;
      const tokens = (await res.json()) as TokenResponse;
      sessionStore.set(sessionFromTokens(tokens, session));
      return tokens.accessToken;
    } catch {
      return null;
    }
  })().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

type RequestOptions = RequestInit & { json?: unknown };

async function send(path: string, init: RequestOptions, token: string | null): Promise<Response> {
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  let body = init.body;
  if (init.json !== undefined) {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(init.json);
  }
  if (!headers.has('Accept')) headers.set('Accept', 'application/json, application/problem+json');
  return fetch(path, { ...init, headers, body });
}

/** fetch with the bearer token, proactive + reactive refresh, and ProblemDetail errors. */
export async function authorizedFetch(path: string, init: RequestOptions = {}): Promise<Response> {
  const session = sessionStore.get();
  let token = session?.accessToken ?? null;
  if (session && session.expiresAt - Date.now() < REFRESH_SKEW_MS) {
    token = (await refreshAccessToken()) ?? token;
  }
  let res = await send(path, init, token);
  if (res.status === 401 && token) {
    const fresh = await refreshAccessToken();
    if (fresh) res = await send(path, init, fresh);
    if (!fresh || res.status === 401) {
      sessionStore.set(null);
      onSessionExpired();
    }
  }
  if (!res.ok) throw await toApiError(res);
  return res;
}

export async function request<T>(path: string, init: RequestOptions = {}): Promise<T> {
  const res = await authorizedFetch(path, init);
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

function qs(params: Record<string, string | number | boolean | null | undefined | Array<string | number>>): string {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v == null || v === '') continue;
    if (Array.isArray(v)) v.forEach((x) => q.append(k, String(x)));
    else q.set(k, String(v));
  }
  const s = q.toString();
  return s ? `?${s}` : '';
}

export interface OrderQuery {
  q?: string;
  status?: OrderStatus[];
  courierId?: number | null;
  page?: number;
  size?: number;
  sort?: string;
}

export interface DisputeQuery {
  status?: DisputeStatus | null;
  statementId?: number | null;
  page?: number;
  size?: number;
}

export interface UploadInput {
  file: File;
  courierId: number;
  periodStart: string;
  periodEnd: string;
}

export const api = {
  /** The signed-in user, with the merchant's display name for the header. */
  me: () => request<{ username: string; merchantName: string | null }>('/api/auth/me'),
  async login(username: string, password: string): Promise<TokenResponse> {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    if (!res.ok) throw await toApiError(res);
    return (await res.json()) as TokenResponse;
  },

  overview: () => request<Overview>('/api/overview'),

  couriers: (query: { q?: string; page?: number; size?: number } = {}) =>
    request<Page<Courier>>(`/api/couriers${qs({ q: query.q, page: query.page ?? 0, size: query.size ?? 100, sort: 'name' })}`),

  orders: (query: OrderQuery) =>
    request<Page<Order>>(`/api/orders${qs({
      q: query.q, status: query.status, courierId: query.courierId, page: query.page ?? 0, size: query.size ?? 20,
      sort: query.sort ?? 'createdAt,desc',
    })}`),

  statements: (page = 0, size = 20) => request<Page<Statement>>(`/api/statements${qs({ page, size })}`),
  statement: (id: number) => request<Statement>(`/api/statements/${id}`),
  reconciliation: (id: number, outcome?: Outcome | null) =>
    request<ReconciliationReport>(`/api/statements/${id}/reconciliation${qs({ outcome })}`),
  upload: (input: UploadInput) => {
    const form = new FormData();
    form.append('file', input.file);
    form.append('courierId', String(input.courierId));
    form.append('periodStart', input.periodStart);
    form.append('periodEnd', input.periodEnd);
    return request<StatementUploadResponse>('/api/statements', { method: 'POST', body: form });
  },

  disputes: (query: DisputeQuery = {}) =>
    request<Page<Dispute>>(`/api/disputes${qs({
      status: query.status, statementId: query.statementId, page: query.page ?? 0, size: query.size ?? 20,
    })}`),
  dispute: (id: number) => request<Dispute>(`/api/disputes/${id}`),
  openDispute: (input: { resultId: number; reason: string; claimedAmount?: string }) =>
    request<Dispute>('/api/disputes', { method: 'POST', json: input }),
  resolveDispute: (id: number, input: { decision: DisputeDecision; resolutionNote?: string }) =>
    request<Dispute>(`/api/disputes/${id}/resolve`, { method: 'POST', json: input }),

  /** The CSV export needs the bearer header, so it is fetched as a blob instead of a plain link. */
  async exportDisputes(status?: DisputeStatus | null): Promise<{ blob: Blob; filename: string }> {
    const res = await authorizedFetch(`/api/disputes/export${qs({ status })}`, { headers: { Accept: 'text/csv' } });
    const disposition = res.headers.get('Content-Disposition') ?? '';
    const filename = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? 'disputes.csv';
    return { blob: await res.blob(), filename };
  },
};
