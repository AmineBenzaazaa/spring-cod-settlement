import { useEffect, useState, type FormEvent } from 'react';
import { toast } from 'sonner';
import { useI18n, type MessageKey } from '@/lib/i18n';
import { absMoney, formatMoney, isValidMoneyInput, normalizeMoneyInput } from '@/lib/money';
import { useOpenDispute } from '@/lib/queries';
import type { Dispute, ReconciliationResult } from '@/lib/types';
import { Button } from './ui/Button';
import { ProblemAlert } from './ui/ErrorState';
import { Modal } from './ui/Sheet';

export function OpenDisputeDialog({ result, onClose, onOpened }: {
  result: ReconciliationResult | null; onClose: () => void; onOpened?: (d: Dispute) => void;
}) {
  const { t, lang } = useI18n();
  const mutation = useOpenDispute();
  const [reason, setReason] = useState('');
  const [claimed, setClaimed] = useState('');
  const [errors, setErrors] = useState<{ reason?: string; claimed?: string }>({});

  // Prefill a sensible reason and the full difference each time a line is picked: one click to submit.
  useEffect(() => {
    if (!result) return;
    mutation.reset();
    setErrors({});
    setClaimed(absMoney(result.difference));
    setReason(t(`dispute.reason.${result.outcome}` as MessageKey, {
      tracking: result.trackingNumber,
      expected: formatMoney(result.expectedAmount, lang),
      collected: formatMoney(result.collectedAmount, lang),
    }));
  }, [result?.id]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!result) return;
    const found: typeof errors = {};
    if (!reason.trim()) found.reason = t('dispute.reasonRequired');
    if (!isValidMoneyInput(claimed)) found.claimed = t('dispute.claimedInvalid');
    setErrors(found);
    if (Object.keys(found).length) return;
    try {
      const dispute = await mutation.mutateAsync({ resultId: result.id, reason: reason.trim(), claimedAmount: normalizeMoneyInput(claimed) });
      toast.success(t('dispute.opened', { id: dispute.id }));
      onOpened?.(dispute);
      onClose();
    } catch {
      /* ProblemAlert below */
    }
  };

  return (
    <Modal open={!!result} onOpenChange={(o) => !o && onClose()} title={t('dispute.new')} testId="dispute-dialog"
      description={result ? t('dispute.newHint', { tracking: result.trackingNumber }) : undefined}>
      {result && (
        <form onSubmit={submit} noValidate>
          <div className="space-y-4 px-5 py-5">
            <div className="space-y-1.5">
              <label htmlFor="reason" className="text-[12.5px] font-medium">{t('dispute.reason')}</label>
              <textarea id="reason" rows={3} className="input h-auto py-2 leading-snug" value={reason} onChange={(e) => setReason(e.target.value)} maxLength={2000} />
              {errors.reason && <p role="alert" className="text-[12px] text-rose-600 dark:text-rose-400">{errors.reason}</p>}
            </div>
            <div className="space-y-1.5">
              <label htmlFor="claimed" className="text-[12.5px] font-medium">{t('dispute.claimed')}</label>
              <div className="relative">
                <input id="claimed" inputMode="decimal" className="input pr-14 font-mono tabular-nums" value={claimed} onChange={(e) => setClaimed(e.target.value)} />
                <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-[12px] font-medium text-faint">MAD</span>
              </div>
              {errors.claimed
                ? <p role="alert" className="text-[12px] text-rose-600 dark:text-rose-400">{errors.claimed}</p>
                : <p className="text-[12px] text-faint">{t('dispute.claimedHint', { amount: formatMoney(absMoney(result.difference), lang) })}</p>}
            </div>
            {mutation.isError && <ProblemAlert error={mutation.error} />}
          </div>
          <div className="flex justify-end gap-2 rounded-b-2xl border-t border-line bg-subtle/50 px-5 py-3">
            <Button type="button" size="sm" onClick={onClose}>{t('dispute.cancel')}</Button>
            <Button type="submit" size="sm" variant="primary" loading={mutation.isPending} data-testid="submit-dispute">{t('dispute.submit')}</Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
