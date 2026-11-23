import { useEffect, useRef } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type DisputeQuery, type OrderQuery, type UploadInput } from './api';
import type { DisputeDecision, Statement, StatementStatus } from './types';

export const keys = {
  me: ['me'] as const,
  overview: ['overview'] as const,
  couriers: ['couriers'] as const,
  courierPage: (q: string, page: number) => ['couriers', 'page', q, page] as const,
  orders: (query: OrderQuery) => ['orders', query] as const,
  statements: (page: number) => ['statements', page] as const,
  statement: (id: number) => ['statement', id] as const,
  reconciliation: (id: number, status?: StatementStatus) => ['reconciliation', id, status] as const,
  disputes: (query: DisputeQuery) => ['disputes', query] as const,
  dispute: (id: number) => ['dispute', id] as const,
};

/** Display name of the signed-in merchant (Atlas Home); null for finance staff. */
export function useMe(enabled = true) {
  return useQuery({ queryKey: keys.me, queryFn: api.me, enabled, staleTime: Infinity, retry: false });
}

/** Statements are processed asynchronously after the upload: keep polling while one is in flight. */
export const POLL_MS = 1_000;
export function isPending(status: StatementStatus | undefined): boolean {
  return status === 'UPLOADED' || status === 'PROCESSING';
}
export function statementPollInterval(statuses: Array<StatementStatus | undefined>): number | false {
  return statuses.some(isPending) ? POLL_MS : false;
}

export function useOverview() {
  return useQuery({ queryKey: keys.overview, queryFn: api.overview });
}

/** All couriers (reference data, a handful): used for names and the upload form. */
export function useCouriers() {
  return useQuery({ queryKey: keys.couriers, queryFn: () => api.couriers(), staleTime: 5 * 60_000 });
}

export function useCourierPage(q: string, page: number) {
  return useQuery({
    queryKey: keys.courierPage(q, page),
    queryFn: () => api.couriers({ q, page, size: 10 }),
    placeholderData: keepPreviousData,
  });
}

export function useOrders(query: OrderQuery) {
  return useQuery({ queryKey: keys.orders(query), queryFn: () => api.orders(query), placeholderData: keepPreviousData });
}

export function useStatements(page: number) {
  return useQuery({
    queryKey: keys.statements(page),
    queryFn: () => api.statements(page),
    placeholderData: keepPreviousData,
    refetchInterval: (query) => statementPollInterval(query.state.data?.content.map((s) => s.status) ?? []),
  });
}

/**
 * One statement, polled until processing finishes. When it leaves UPLOADED/PROCESSING the
 * dashboard numbers changed, so the overview and the history are invalidated once.
 */
export function useStatement(id: number) {
  const qc = useQueryClient();
  const query = useQuery({
    queryKey: keys.statement(id),
    queryFn: () => api.statement(id),
    refetchInterval: (q) => statementPollInterval([q.state.data?.status]),
  });
  const previous = useRef<StatementStatus | undefined>(undefined);
  const status = query.data?.status;
  useEffect(() => {
    if (isPending(previous.current) && status && !isPending(status)) {
      qc.invalidateQueries({ queryKey: keys.overview });
      qc.invalidateQueries({ queryKey: ['statements'] });
    }
    previous.current = status;
  }, [status, qc]);
  return query;
}

/** The report is only meaningful once reconciled; the status in the key refetches it on change. */
export function useReconciliation(statement: Statement | undefined) {
  return useQuery({
    queryKey: keys.reconciliation(statement?.id ?? 0, statement?.status),
    queryFn: () => api.reconciliation(statement!.id),
    enabled: statement?.status === 'RECONCILED',
  });
}

export function useDisputes(query: DisputeQuery) {
  return useQuery({ queryKey: keys.disputes(query), queryFn: () => api.disputes(query), placeholderData: keepPreviousData });
}

export function useDispute(id: number | null) {
  return useQuery({ queryKey: keys.dispute(id ?? 0), queryFn: () => api.dispute(id!), enabled: id != null });
}

export function useUpload() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: UploadInput) => api.upload(input),
    onSuccess: (res) => {
      qc.setQueryData(keys.statement(res.statement.id), res.statement);
      qc.invalidateQueries({ queryKey: ['statements'] });
    },
  });
}

export function useOpenDispute() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.openDispute,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['reconciliation'] });
      qc.invalidateQueries({ queryKey: ['disputes'] });
      qc.invalidateQueries({ queryKey: keys.overview });
    },
  });
}

export function useResolveDispute() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, decision, resolutionNote }: { id: number; decision: DisputeDecision; resolutionNote?: string }) =>
      api.resolveDispute(id, { decision, resolutionNote }),
    onSuccess: (dispute) => {
      qc.setQueryData(keys.dispute(dispute.id), dispute);
      qc.invalidateQueries({ queryKey: ['disputes'] });
      qc.invalidateQueries({ queryKey: ['reconciliation'] });
      qc.invalidateQueries({ queryKey: keys.overview });
    },
  });
}
