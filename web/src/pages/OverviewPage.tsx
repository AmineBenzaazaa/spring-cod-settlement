import { Link } from 'react-router-dom';
import { ArrowRight, Upload } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { useOverview } from '@/lib/queries';
import { KpiTilesSkeleton, KpiTilesView } from '@/components/KpiTiles';
import { BalanceChart, CourierBalanceTable } from '@/components/CourierBalances';
import { Button } from '@/components/ui/Button';
import { ErrorState } from '@/components/ui/ErrorState';
import { PageHeader } from '@/components/ui/PageHeader';
import { Skeleton } from '@/components/ui/Skeleton';

export function OverviewPage() {
  const { t } = useI18n();
  const { isStaff } = useAuth();
  const overview = useOverview();

  return (
    <>
      <PageHeader
        title={t('overview.title')}
        subtitle={isStaff ? t('overview.subtitleStaff') : t('overview.subtitleMerchant')}
        actions={!isStaff && (
          <Link to="/statements"><Button size="sm" variant="primary"><Upload className="h-3.5 w-3.5" />{t('upload.title')}</Button></Link>
        )}
      />
      {overview.isError ? (
        <div className="panel"><ErrorState error={overview.error} onRetry={() => overview.refetch()} /></div>
      ) : !overview.data ? (
        <>
          <KpiTilesSkeleton />
          <div className="panel space-y-3 p-4"><Skeleton className="h-4 w-40" /><Skeleton className="h-40" /></div>
        </>
      ) : (
        <>
          <KpiTilesView overview={overview.data} />
          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
            <section className="panel min-w-0" aria-labelledby="balances-title">
              <div className="flex items-start justify-between gap-3 border-b border-line px-4 py-3">
                <div>
                  <h2 id="balances-title" className="text-[14px] font-semibold">{t('balances.title')}</h2>
                  <p className="mt-0.5 text-[12px] text-muted">{t('balances.subtitle')}</p>
                </div>
                <Link to="/couriers" className="focus-ring inline-flex items-center gap-1 rounded text-[12px] font-medium text-muted hover:text-fg">
                  {t('nav.couriers')}<ArrowRight className="h-3 w-3" />
                </Link>
              </div>
              <CourierBalanceTable couriers={overview.data.couriers} />
            </section>
            <section className="panel p-4" aria-labelledby="chart-title">
              <div className="mb-2 flex items-center justify-between">
                <h2 id="chart-title" className="text-[14px] font-semibold">{t('balances.chart')}</h2>
                <div className="flex items-center gap-3 text-[11px] text-muted">
                  <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-accent/25" />{t('balances.expected')}</span>
                  <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-signal" />{t('balances.reported')}</span>
                </div>
              </div>
              <BalanceChart couriers={overview.data.couriers} />
            </section>
          </div>
        </>
      )}
    </>
  );
}
