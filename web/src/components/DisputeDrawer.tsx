import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Clock, X as XIcon } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/lib/auth';
import { useI18n, type MessageKey } from '@/lib/i18n';
import { formatDateTime } from '@/lib/format';
import { useDispute, useResolveDispute } from '@/lib/queries';
import type { DisputeDecision } from '@/lib/types';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import { ErrorState, ProblemAlert } from './ui/ErrorState';
import { Money } from './ui/Money';
import { Sheet } from './ui/Sheet';
import { Skeleton } from './ui/Skeleton';

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="min-w-0">
      <dt className="label">{label}</dt>
      <dd className="mt-1 text-[13px]">{children}</dd>
    </div>
  );
}

/** Detail drawer. Resolution controls are rendered for STAFF only (the API enforces it too: 403). */
export function DisputeDrawer({ disputeId, onClose }: { disputeId: number | null; onClose: () => void }) {
  const { t, lang } = useI18n();
  const { isStaff } = useAuth();
  const dispute = useDispute(disputeId);
  const resolve = useResolveDispute();
  const [note, setNote] = useState('');

  useEffect(() => {
    setNote('');
    resolve.reset();
  }, [disputeId]);

  const decide = async (decision: DisputeDecision) => {
    if (!disputeId) return;
    try {
      const d = await resolve.mutateAsync({ id: disputeId, decision, resolutionNote: note.trim() || undefined });
      toast.success(t('drawer.resolved', { id: d.id, status: t(`status.${d.status}` as MessageKey).toLowerCase() }));
    } catch {
      /* ProblemAlert (e.g. 409 concurrent update) */
    }
  };

  const d = dispute.data;
  return (
    <Sheet open={disputeId != null} onClose={onClose} testId="dispute-drawer"
      title={<span className="flex items-center gap-2">#{disputeId}{d && <Badge value={d.status} prefix="status" />}</span>}>
      {dispute.isError ? <ErrorState error={dispute.error} onRetry={() => dispute.refetch()} /> : !d ? (
        <div className="space-y-3 p-5"><Skeleton className="h-6 w-40" /><Skeleton className="h-24" /></div>
      ) : (
        <>
          <section className="border-b border-line px-5 py-4">
            <p className="font-mono text-[15px] font-semibold tracking-tight">{d.trackingNumber}</p>
            <div className="mt-1.5"><Badge value={d.outcome} prefix="outcome" /></div>
            <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-3">
              <Field label={t('disputes.claimed')}><Money value={d.claimedAmount} className="text-[15px] font-semibold" /></Field>
              <Field label={t('disputes.statement')}>
                <Link to={`/statements/${d.statementId}`} className="font-medium text-accent hover:underline" onClick={onClose}>
                  {t('drawer.viewStatement', { id: d.statementId })}
                </Link>
              </Field>
              <Field label={t('disputes.opened')}>{formatDateTime(d.createdAt, lang)}</Field>
              <Field label={t('drawer.resolvedAt')}>{formatDateTime(d.resolvedAt, lang)}</Field>
            </dl>
          </section>
          <section className="border-b border-line px-5 py-4">
            <p className="label mb-1.5">{t('drawer.reason')}</p>
            <p className="whitespace-pre-wrap text-[13px] leading-relaxed" data-testid="dispute-reason">{d.reason}</p>
            {d.resolutionNote && (
              <>
                <p className="label mb-1.5 mt-4">{t('drawer.resolution')}</p>
                <p className="whitespace-pre-wrap text-[13px] leading-relaxed">{d.resolutionNote}</p>
              </>
            )}
          </section>
          {d.status === 'OPEN' && (isStaff ? (
            <section className="space-y-3 px-5 py-4" data-testid="resolve-panel">
              <div>
                <p className="text-[13px] font-semibold">{t('drawer.resolveTitle')}</p>
                <p className="mt-0.5 text-[12px] text-muted">{t('drawer.resolveHint')}</p>
              </div>
              <div className="space-y-1.5">
                <label htmlFor="note" className="text-[12.5px] font-medium">{t('drawer.note')}</label>
                <textarea id="note" rows={3} className="input h-auto py-2" value={note} onChange={(e) => setNote(e.target.value)} maxLength={2000} />
              </div>
              {resolve.isError && <ProblemAlert error={resolve.error} />}
              <div className="flex flex-wrap gap-2">
                <Button size="sm" variant="primary" loading={resolve.isPending && resolve.variables?.decision === 'RESOLVED'}
                  disabled={resolve.isPending} onClick={() => decide('RESOLVED')} data-testid="resolve-dispute">
                  <Check className="h-3.5 w-3.5" />{t('drawer.resolve')}
                </Button>
                <Button size="sm" variant="ghost" loading={resolve.isPending && resolve.variables?.decision === 'REJECTED'}
                  disabled={resolve.isPending} onClick={() => decide('REJECTED')} data-testid="reject-dispute"
                  className="text-rose-600 hover:bg-rose-500/10 hover:text-rose-600 dark:text-rose-400">
                  <XIcon className="h-3.5 w-3.5" />{t('drawer.reject')}
                </Button>
              </div>
            </section>
          ) : (
            <p className="flex items-center gap-2 px-5 py-4 text-[12.5px] text-muted" data-testid="awaiting-staff">
              <Clock className="h-3.5 w-3.5" />{t('drawer.waiting')}
            </p>
          ))}
        </>
      )}
    </Sheet>
  );
}
