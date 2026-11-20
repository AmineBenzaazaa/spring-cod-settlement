import { useNavigate } from 'react-router-dom';
import { FileSpreadsheet, RefreshCw } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { formatDateTime, formatPeriod, relativeTime } from '@/lib/format';
import { isPending, useCouriers, useStatements } from '@/lib/queries';
import { Badge } from './ui/Badge';
import { EmptyState } from './ui/EmptyState';
import { ErrorState } from './ui/ErrorState';
import { TableSkeletonRows } from './ui/PageHeader';
import { Pager } from './ui/Pager';

export function StatementsTable({ page, onPage, highlight }: { page: number; onPage: (p: number) => void; highlight?: number | null }) {
  const { t, lang } = useI18n();
  const { isStaff } = useAuth();
  const navigate = useNavigate();
  const statements = useStatements(page);
  const couriers = useCouriers();
  const courierName = (id: number) => couriers.data?.content.find((c) => c.id === id)?.name ?? `#${id}`;
  const polling = statements.data?.content.some((s) => isPending(s.status));
  const cols = isStaff ? 8 : 7;

  return (
    <section className="panel flex min-w-0 flex-col" aria-labelledby="history-title">
      <div className="flex items-center justify-between border-b border-line px-4 py-3">
        <h2 id="history-title" className="text-[14px] font-semibold">{t('history.title')}</h2>
        {polling && (
          <span className="inline-flex items-center gap-1.5 text-[12px] text-sky-600 dark:text-sky-300" data-testid="polling">
            <RefreshCw className="h-3 w-3 animate-spin" />{t('history.polling')}
          </span>
        )}
      </div>
      {statements.isError ? (
        <ErrorState error={statements.error} onRetry={() => statements.refetch()} />
      ) : statements.data && statements.data.totalElements === 0 ? (
        <EmptyState icon={<FileSpreadsheet className="h-4 w-4" />} title={t('history.empty')} hint={t('history.emptyHint')} />
      ) : (
        <div className="scrollbar-thin overflow-x-auto">
          <table className="w-full" data-testid="statements-table">
            <thead>
              <tr>
                <th className="th">{t('history.id')}</th>
                <th className="th">{t('history.file')}</th>
                <th className="th">{t('history.courier')}</th>
                {isStaff && <th className="th">{t('history.merchant')}</th>}
                <th className="th">{t('history.period')}</th>
                <th className="th text-right">{t('history.lines')}</th>
                <th className="th">{t('history.status')}</th>
                <th className="th">{t('history.uploaded')}</th>
              </tr>
            </thead>
            <tbody>
              {!statements.data ? <TableSkeletonRows cols={cols} rows={4} /> : statements.data.content.map((s) => (
                <tr key={s.id} tabIndex={0} data-testid="statement-row" data-id={s.id} data-status={s.status}
                  onClick={() => navigate(`/statements/${s.id}`)}
                  onKeyDown={(e) => e.key === 'Enter' && navigate(`/statements/${s.id}`)}
                  className={`focus-ring cursor-pointer border-t border-line hover:bg-subtle/60 ${highlight === s.id ? 'bg-accent/[0.05]' : ''}`}>
                  <td className="td font-mono text-muted">#{s.id}</td>
                  <td className="td max-w-[220px]">
                    <p className="truncate font-mono text-[12.5px]" title={s.originalFilename}>{s.originalFilename}</p>
                    <p className="font-mono text-[10.5px] text-faint" title={s.fileSha256}>{s.fileSha256.slice(0, 12)}…</p>
                  </td>
                  <td className="td">{courierName(s.courierId)}</td>
                  {isStaff && <td className="td font-mono text-muted">#{s.merchantId}</td>}
                  <td className="td text-muted">{formatPeriod(s.periodStart, s.periodEnd, lang)}</td>
                  <td className="td num text-muted">{s.lineCount || '—'}</td>
                  <td className="td"><Badge value={s.status} prefix="status" pulse={isPending(s.status)} /></td>
                  <td className="td text-muted" title={formatDateTime(s.uploadedAt, lang)}>{relativeTime(s.uploadedAt, lang)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {statements.data && <Pager page={statements.data} onPage={onPage} fetching={statements.isPlaceholderData} />}
    </section>
  );
}
