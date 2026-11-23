import type { ReactNode } from 'react';
import { AlertTriangle, Banknote, Gavel, HandCoins, Receipt } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatNumber } from '@/lib/format';
import { formatMoney, formatPercent, ratioPercent } from '@/lib/money';
import type { Overview } from '@/lib/types';
import { Money } from './ui/Money';
import { Skeleton } from './ui/Skeleton';
import { Term } from './ui/Term';

function Tile({ label, value, hint, icon, tone, testId }: {
  label: ReactNode; value: ReactNode; hint: string; icon: ReactNode; tone: string; testId: string;
}) {
  return (
    <div className="panel flex min-w-0 flex-col gap-2 p-4" data-testid={testId}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-[12px] font-medium text-muted">{label}</span>
        <span className={`flex h-6 w-6 items-center justify-center rounded-md ${tone}`}>{icon}</span>
      </div>
      <div className="truncate text-[24px] font-semibold leading-none tracking-tight tabular-nums">{value}</div>
      <p className="text-[12px] leading-snug text-faint">{hint}</p>
    </div>
  );
}

/** One icon-tile style (navy tint); colour only where the state means something. */
const NAVY = 'bg-accent/10 text-accent';

const big = (v: string) => <Money value={v} className="font-sans" />;

export function KpiTilesView({ overview }: { overview: Overview }) {
  const { t, lang } = useI18n();
  const delivered = overview.couriers.reduce((n, c) => n + c.deliveredOrders, 0);
  const pct = ratioPercent(overview.reportedCollections, overview.expectedCollections);
  return (
    <div className="space-y-2">
    <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5">
      <Tile testId="kpi-expected" label={<Term k="expected">{t('kpi.expected')}</Term>} value={big(overview.expectedCollections)}
        hint={t('kpi.expectedHint', { n: formatNumber(delivered, lang) })}
        icon={<Receipt className="h-3.5 w-3.5" />} tone={NAVY} />
      <Tile testId="kpi-reported" label={<Term k="reported">{t('kpi.reported')}</Term>} value={big(overview.reportedCollections)}
        hint={t('kpi.reportedHint', { pct: formatPercent(pct, lang) })}
        icon={<HandCoins className="h-3.5 w-3.5" />} tone={NAVY} />
      <Tile testId="kpi-net" label={<Term k="netRemitted">{t('kpi.net')}</Term>} value={big(overview.netRemitted)}
        hint={t('kpi.netHint', { fees: formatMoney(overview.fees, lang) })}
        icon={<Banknote className="h-3.5 w-3.5" />} tone="bg-signal/12 text-signal-ink" />
      <Tile testId="kpi-disputes" label={<Term k="dispute">{t('kpi.disputes')}</Term>} value={formatNumber(overview.openDisputes, lang)}
        hint={t('kpi.disputesHint', { amount: formatMoney(overview.openDisputedAmount, lang) })}
        icon={<Gavel className="h-3.5 w-3.5" />} tone={NAVY} />
      <Tile testId="kpi-discrepancy" label={<Term k="discrepancy">{t('kpi.discrepancy')}</Term>} value={big(overview.discrepancyAmount)}
        hint={t('kpi.discrepancyHint', { n: formatNumber(overview.discrepancies, lang) })}
        icon={<AlertTriangle className="h-3.5 w-3.5" />} tone="bg-danger/10 text-danger" />
    </div>
    <p className="text-[12px] text-muted" data-testid="kpi-explain">{t('kpi.explain')}</p>
    </div>
  );
}

export function KpiTilesSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5" data-testid="kpi-loading">
      {Array.from({ length: 5 }, (_, i) => (
        <div key={i} className="panel space-y-3 p-4">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-7 w-28" />
          <Skeleton className="h-3 w-32" />
        </div>
      ))}
    </div>
  );
}
