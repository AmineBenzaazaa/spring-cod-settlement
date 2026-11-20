import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Loader2, XCircle } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatDateTime, formatPeriod } from '@/lib/format';
import { isPending, useCouriers, useReconciliation, useStatement } from '@/lib/queries';
import type { ReconciliationResult } from '@/lib/types';
import { OpenDisputeDialog } from '@/components/OpenDisputeDialog';
import { ReconciliationTable } from '@/components/ReconciliationTable';
import { Badge } from '@/components/ui/Badge';
import { CopyButton } from '@/components/ui/CopyButton';
import { ErrorState } from '@/components/ui/ErrorState';
import { Money } from '@/components/ui/Money';
import { PageHeader } from '@/components/ui/PageHeader';
import { Skeleton } from '@/components/ui/Skeleton';

function Stat({ label, children, testId }: { label: string; children: React.ReactNode; testId?: string }) {
  return (
    <div className="min-w-0 px-4 py-3" data-testid={testId}>
      <p className="label">{label}</p>
      <p className="mt-1 truncate text-[17px] font-semibold tracking-tight">{children}</p>
    </div>
  );
}

export function StatementDetailPage() {
  const { t, lang } = useI18n();
  const id = Number(useParams().id);
  const statement = useStatement(id);
  const report = useReconciliation(statement.data);
  const couriers = useCouriers();
  const [disputing, setDisputing] = useState<ReconciliationResult | null>(null);

  const s = statement.data;
  const courier = couriers.data?.content.find((c) => c.id === s?.courierId);
  const back = (
    <Link to="/statements" className="focus-ring mb-1.5 inline-flex items-center gap-1 rounded text-[12px] font-medium text-muted hover:text-fg">
      <ArrowLeft className="h-3 w-3" />{t('recon.back')}
    </Link>
  );

  if (statement.isError) {
    return <>{back}<div className="panel"><ErrorState error={statement.error} onRetry={() => statement.refetch()} /></div></>;
  }

  return (
    <>
      <PageHeader
        eyebrow={back}
        title={<span className="flex items-center gap-2.5">{t('recon.title', { id })}{s && <Badge value={s.status} prefix="status" pulse={isPending(s.status)} />}</span>}
        subtitle={s ? (
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-mono">{s.originalFilename}</span><span className="text-faint">·</span>
            <span>{courier?.name ?? `#${s.courierId}`}</span><span className="text-faint">·</span>
            <span>{formatPeriod(s.periodStart, s.periodEnd, lang)}</span><span className="text-faint">·</span>
            <span title={formatDateTime(s.uploadedAt, lang)}>{s.lineCount} lines</span>
            <span className="inline-flex items-center gap-1 rounded bg-subtle px-1.5 py-0.5 font-mono text-[11px] text-muted" title={s.fileSha256}>
              {t('recon.sha')} {s.fileSha256.slice(0, 10)}…<CopyButton value={s.fileSha256} />
            </span>
          </span>
        ) : <Skeleton className="h-4 w-80" />}
      />

      {s && isPending(s.status) && (
        <div className="panel flex items-center gap-3 p-5" data-testid="processing">
          <Loader2 className="h-5 w-5 animate-spin text-sky-500" />
          <div>
            <p className="text-[14px] font-medium">{t('recon.processing')}</p>
            <p className="text-[12.5px] text-muted">{t('recon.processingHint')}</p>
          </div>
        </div>
      )}

      {s?.status === 'FAILED' && (
        <div role="alert" className="panel flex items-start gap-3 border-rose-500/30 p-5">
          <XCircle className="mt-0.5 h-5 w-5 text-rose-500" />
          <div>
            <p className="text-[14px] font-medium">{t('recon.failed')}</p>
            <p className="text-[12.5px] text-muted">{s.errorMessage}</p>
          </div>
        </div>
      )}

      {report.isError && <div className="panel"><ErrorState error={report.error} onRetry={() => report.refetch()} /></div>}

      {s?.status === 'RECONCILED' && !report.data && !report.isError && (
        <div className="panel space-y-3 p-4"><Skeleton className="h-12" /><Skeleton className="h-64" /></div>
      )}

      {report.data?.summary && (
        <>
          <div className="panel grid grid-cols-2 divide-line sm:grid-cols-5 sm:divide-x" data-testid="recon-summary">
            <Stat label={t('recon.expected')}><Money value={report.data.summary.expectedTotal} /></Stat>
            <Stat label={t('recon.collected')}><Money value={report.data.summary.collectedTotal} /></Stat>
            <Stat label={t('recon.fees')}><Money value={report.data.summary.feesTotal} muted /></Stat>
            <Stat label={t('recon.net')}><Money value={report.data.summary.netRemitted} /></Stat>
            <Stat label={t('recon.difference')} testId="summary-difference"><Money value={report.data.summary.differenceTotal} diff /></Stat>
          </div>
          <ReconciliationTable results={report.data.results} summary={report.data.summary} onOpenDispute={setDisputing} />
        </>
      )}

      <OpenDisputeDialog result={disputing} onClose={() => setDisputing(null)} />
    </>
  );
}
