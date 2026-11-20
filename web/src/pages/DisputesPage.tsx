import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import clsx from 'clsx';
import { Download, Gavel } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { saveBlob } from '@/lib/download';
import { useI18n } from '@/lib/i18n';
import { formatDateTime, relativeTime } from '@/lib/format';
import { useDisputes } from '@/lib/queries';
import type { DisputeStatus } from '@/lib/types';
import { DisputeDrawer } from '@/components/DisputeDrawer';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState, ProblemAlert } from '@/components/ui/ErrorState';
import { Money } from '@/components/ui/Money';
import { PageHeader, TableSkeletonRows } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';

const STATUSES: Array<DisputeStatus | null> = [null, 'OPEN', 'RESOLVED', 'REJECTED'];

export function DisputesPage() {
  const { t, lang } = useI18n();
  const { isStaff } = useAuth();
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') as DisputeStatus | null) ?? null;
  const selected = params.get('id') ? Number(params.get('id')) : null;
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<unknown>(null);
  const disputes = useDisputes({ status, page });

  const update = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) if (v == null) p.delete(k); else p.set(k, v);
    setParams(p, { replace: true });
  };

  const exportCsv = async () => {
    setExporting(true);
    setExportError(null);
    try {
      const { blob, filename } = await api.exportDisputes(status);
      saveBlob(blob, filename);
      toast.success(t('disputes.exported', { file: filename }));
    } catch (err) {
      setExportError(err);
    } finally {
      setExporting(false);
    }
  };

  return (
    <>
      <PageHeader title={t('disputes.title')} subtitle={t('disputes.subtitle')}
        actions={<Button size="sm" onClick={exportCsv} loading={exporting} data-testid="export-csv">{!exporting && <Download className="h-3.5 w-3.5" />}{t('disputes.export')}</Button>} />
      {exportError != null && <ProblemAlert error={exportError} />}
      <section className="panel min-w-0">
        <div className="flex flex-wrap gap-1.5 border-b border-line p-3">
          {STATUSES.map((s) => (
            <button key={s ?? 'ALL'} type="button" aria-pressed={status === s} data-testid={`dispute-filter-${s ?? 'ALL'}`}
              onClick={() => { setPage(0); update({ status: s }); }}
              className={clsx('focus-ring rounded-md border px-2.5 py-1 text-[12px] font-medium transition-colors',
                status === s ? 'border-fg/20 bg-fg text-bg' : 'border-line bg-panel text-muted hover:bg-subtle hover:text-fg')}>
              {s ? t(`status.${s}`) : t('disputes.filterAll')}
            </button>
          ))}
        </div>
        {disputes.isError ? <ErrorState error={disputes.error} onRetry={() => disputes.refetch()} />
          : disputes.data?.totalElements === 0 ? (
            <EmptyState icon={<Gavel className="h-4 w-4" />} title={t('disputes.empty')} hint={t('disputes.emptyHint')} />
          ) : (
            <div className="scrollbar-thin overflow-x-auto">
              <table className="w-full" data-testid="disputes-table">
                <thead>
                  <tr>
                    <th className="th">{t('disputes.id')}</th>
                    <th className="th">{t('recon.tracking')}</th>
                    <th className="th">{t('recon.outcome')}</th>
                    <th className="th">{t('disputes.statement')}</th>
                    {isStaff && <th className="th">{t('history.merchant')}</th>}
                    <th className="th text-right">{t('disputes.claimed')}</th>
                    <th className="th">{t('history.status')}</th>
                    <th className="th">{t('disputes.opened')}</th>
                  </tr>
                </thead>
                <tbody>
                  {!disputes.data ? <TableSkeletonRows cols={isStaff ? 8 : 7} /> : disputes.data.content.map((d) => (
                    <tr key={d.id} tabIndex={0} data-testid="dispute-row" data-id={d.id} data-status={d.status}
                      onClick={() => update({ id: String(d.id) })} onKeyDown={(e) => e.key === 'Enter' && update({ id: String(d.id) })}
                      className={clsx('focus-ring cursor-pointer border-t border-line hover:bg-subtle/60', selected === d.id && 'bg-accent/[0.05]')}>
                      <td className="td font-mono text-muted">#{d.id}</td>
                      <td className="td font-mono text-[12.5px] font-medium">{d.trackingNumber}</td>
                      <td className="td"><Badge value={d.outcome} prefix="outcome" /></td>
                      <td className="td">
                        <Link to={`/statements/${d.statementId}`} onClick={(e) => e.stopPropagation()} className="font-mono text-accent hover:underline">#{d.statementId}</Link>
                      </td>
                      {isStaff && <td className="td font-mono text-muted">#{d.merchantId}</td>}
                      <td className="td num"><Money value={d.claimedAmount} /></td>
                      <td className="td"><Badge value={d.status} prefix="status" /></td>
                      <td className="td text-muted" title={formatDateTime(d.createdAt, lang)}>{relativeTime(d.createdAt, lang)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        {disputes.data && <Pager page={disputes.data} onPage={setPage} fetching={disputes.isPlaceholderData} />}
      </section>
      <DisputeDrawer disputeId={selected} onClose={() => update({ id: null })} />
    </>
  );
}
