import { vi } from 'vitest';
import type { ReactNode } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { api } from './api';
import { POLL_MS, statementPollInterval, useStatement } from './queries';
import type { Statement } from './types';
import { newQueryClient } from '@/test/utils';

const statement = (status: Statement['status']): Statement => ({
  id: 7, merchantId: 1, courierId: 1, periodStart: '2026-10-01', periodEnd: '2026-10-31', originalFilename: 'oct.csv',
  fileSha256: 'ab'.repeat(32), currency: 'MAD', status, errorMessage: null, lineCount: 13, uploadedAt: '2026-11-01T10:00:00Z',
  reconciledAt: null,
});

describe('statement polling', () => {
  it('polls only while a statement is queued or processing', () => {
    expect(statementPollInterval(['UPLOADED'])).toBe(POLL_MS);
    expect(statementPollInterval(['RECONCILED', 'PROCESSING'])).toBe(POLL_MS);
    expect(statementPollInterval(['RECONCILED', 'FAILED'])).toBe(false);
    expect(statementPollInterval([])).toBe(false);
  });

  it('useStatement keeps polling until RECONCILED, then refreshes the overview once', async () => {
    const spy = vi.spyOn(api, 'statement')
      .mockResolvedValueOnce(statement('UPLOADED'))
      .mockResolvedValueOnce(statement('PROCESSING'))
      .mockResolvedValue(statement('RECONCILED'));
    const client = newQueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;

    const { result } = renderHook(() => useStatement(7), { wrapper });

    await waitFor(() => expect(result.current.data?.status).toBe('RECONCILED'), { timeout: 5_000 });
    const calls = spy.mock.calls.length;
    expect(calls).toBe(3);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['overview'] });

    await new Promise((r) => setTimeout(r, POLL_MS + 200));
    expect(spy.mock.calls.length).toBe(calls); // stopped polling
    spy.mockRestore();
  }, 10_000);
});
