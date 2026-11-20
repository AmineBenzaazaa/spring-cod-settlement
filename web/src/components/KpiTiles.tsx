import type { ReactNode } from 'react';
import { AlertTriangle, Banknote, Gavel, HandCoins, Receipt } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatNumber } from '@/lib/format';
import { formatMoney, ratioPercent } from '@/lib/money';
import type { Overview } from '@/lib/types';
import { Money } from './ui/Money';
import { Skeleton } from './ui/Skeleton';

function Tile({ label, value, hint, icon, tone, testId }: {
  label: string; value: ReactNode; hint: string; icon: ReactNode; tone: string; testId: string;
}) {
  return (
    <div className="panel flex min-w-0 flex-col gap-2 p-4" data-testid={testId}>
      <div className="flex items-center justify-between">
        <span className="text-[12px] font-medium text-muted">{label}</span>
        <span className={`flex h-6 w-6 items-center justify-center rounded-md ${tone}`}>{icon}</span>
      </div>
      <div className="truncate text-[24px] font-semibold leading-none tracking-tight tabular-nums">{value}</div>
      <p className="truncate text-[12px] text-faint" title={hint}>{hint}</p>
    </div>
  );
}

const big = (v: string) => <Money value={v} className="font-sans" />;

export function KpiTilesView({ overview }: { overview: Overview }) {
  const { t, lang } = useI18n();
  const delivered = overview.couriers.reduce((n, c) => n + c.deliveredOrders, 0);
  const pct = ratioPercent(overview.reportedCollections, overview.expectedCollections);
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5">
      <Tile testId="kpi-expected" label={t('kpi.expected')} value={big(overview.expectedCollections)}
        hint={t('kpi.expectedHint', { n: formatNumber(delivered, lang) })}
        icon={<Receipt className="h-3.5 w-3.5" />} tone="bg-zinc-500/10 text-zinc-600 dark:text-zinc-300" />
      <Tile testId="kpi-reported" label={t('kpi.reported')} value={big(overview.reportedCollections)}
        hint={t('kpi.reportedHint', { pct: pct == null ? '—' : `${pct.replace('.', lang === 'fr' ? ',' : '.')} %` })}
        icon={<HandCoins className="h-3.5 w-3.5" />} tone="bg-sky-500/10 text-sky-600 dark:text-sky-300" />
      <Tile testId="kpi-net" label={t('kpi.net')} value={big(overview.netRemitted)}
        hint={t('kpi.netHint', { fees: formatMoney(overview.fees, lang) })}
        icon={<Banknote className="h-3.5 w-3.5" />} tone="bg-emerald-500/10 text-emerald-600 dark:text-emerald-300" />
      <Tile testId="kpi-disputes" label={t('kpi.disputes')} value={formatNumber(overview.openDisputes, lang)}
        hint={t('kpi.disputesHint', { amount: formatMoney(overview.openDisputedAmount, lang) })}
        icon={<Gavel className="h-3.5 w-3.5" />} tone="bg-amber-500/10 text-amber-600 dark:text-amber-300" />
      <Tile testId="kpi-discrepancy" label={t('kpi.discrepancy')} value={big(overview.discrepancyAmount)}
        hint={t('kpi.discrepancyHint', { n: formatNumber(overview.discrepancies, lang) })}
        icon={<AlertTriangle className="h-3.5 w-3.5" />} tone="bg-rose-500/10 text-rose-600 dark:text-rose-300" />
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
