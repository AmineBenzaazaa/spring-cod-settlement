import { vi } from 'vitest';
import { api, ApiError, onSessionExpiredCallback } from './api';
import { sessionStore } from './session';
import { fakeJwt, jsonResponse, problem, signIn } from '@/test/utils';

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

const auth = (call: number) => new Headers(fetchMock.mock.calls[call]![1]!.headers).get('Authorization');
const url = (call: number) => String(fetchMock.mock.calls[call]![0]);

const newTokens = () => ({
  accessToken: fakeJwt({ sub: 'atlas', role: 'MERCHANT', mid: 1, exp: Math.floor(Date.now() / 1000) + 900 }),
  refreshToken: 'refresh-2', tokenType: 'Bearer', expiresIn: 900,
});

describe('api client', () => {
  it('sends the bearer token', async () => {
    signIn();
    fetchMock.mockResolvedValueOnce(jsonResponse({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    await api.statements();
    expect(auth(0)).toBe('Bearer access-1');
  });

  it('refreshes once on 401 and retries with the new token', async () => {
    signIn();
    const tokens = newTokens();
    fetchMock
      .mockResolvedValueOnce(problem(401, 'Unauthorized', 'Token expired'))
      .mockResolvedValueOnce(jsonResponse(tokens))
      .mockResolvedValueOnce(jsonResponse({ id: 1 }));

    await expect(api.dispute(1)).resolves.toEqual({ id: 1 });

    expect(url(1)).toBe('/api/auth/refresh');
    expect(JSON.parse(String(fetchMock.mock.calls[1]![1]!.body))).toEqual({ refreshToken: 'refresh-1' });
    expect(auth(2)).toBe(`Bearer ${tokens.accessToken}`);
    expect(sessionStore.get()?.refreshToken).toBe('refresh-2');
  });

  it('sends a single refresh for concurrent 401s (single flight)', async () => {
    signIn();
    const tokens = newTokens();
    fetchMock.mockImplementation(async (input, init) => {
      if (String(input) === '/api/auth/refresh') return jsonResponse(tokens);
      const bearer = new Headers(init?.headers).get('Authorization');
      return bearer === 'Bearer access-1' ? problem(401, 'Unauthorized', 'expired') : jsonResponse({ ok: true });
    });

    await Promise.all([api.dispute(1), api.dispute(2), api.overview()]);

    expect(fetchMock.mock.calls.filter(([u]) => String(u) === '/api/auth/refresh')).toHaveLength(1);
  });

  it('refreshes proactively when the access token is about to expire', async () => {
    signIn({ expiresAt: Date.now() + 5_000 });
    const tokens = newTokens();
    fetchMock.mockResolvedValueOnce(jsonResponse(tokens)).mockResolvedValueOnce(jsonResponse({ id: 3 }));

    await api.dispute(3);

    expect(url(0)).toBe('/api/auth/refresh');
    expect(auth(1)).toBe(`Bearer ${tokens.accessToken}`);
  });

  it('signs out when the refresh token is rejected too', async () => {
    signIn();
    const expired = vi.fn();
    onSessionExpiredCallback(expired);
    fetchMock
      .mockResolvedValueOnce(problem(401, 'Unauthorized', 'expired'))
      .mockResolvedValueOnce(problem(401, 'Unauthorized', 'refresh expired'));

    await expect(api.overview()).rejects.toBeInstanceOf(ApiError);

    expect(sessionStore.get()).toBeNull();
    expect(expired).toHaveBeenCalledOnce();
  });

  it('turns a ProblemDetail into an ApiError with field errors', async () => {
    signIn();
    fetchMock.mockResolvedValueOnce(problem(400, 'Invalid request', 'The request contains invalid data.', {
      file: ['Row 2: tracking_number is required.', 'Row 3: amount_collected is not a valid amount.'],
    }));

    const error = await api.upload({ file: new File(['x'], 'a.csv'), courierId: 1, periodStart: '2026-10-01', periodEnd: '2026-10-31' })
      .catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 400, title: 'Invalid request', detail: 'The request contains invalid data.' });
    expect((error as ApiError).fieldErrors.file).toHaveLength(2);
  });

  it('sends uploads as multipart with the period and courier', async () => {
    signIn();
    fetchMock.mockResolvedValueOnce(jsonResponse({ duplicate: true, statement: { id: 1 } }));
    await api.upload({ file: new File(['a'], 'oct.csv'), courierId: 1, periodStart: '2026-10-01', periodEnd: '2026-10-31' });
    const body = fetchMock.mock.calls[0]![1]!.body as FormData;
    expect(body.get('courierId')).toBe('1');
    expect(body.get('periodStart')).toBe('2026-10-01');
    expect((body.get('file') as File).name).toBe('oct.csv');
  });
});
