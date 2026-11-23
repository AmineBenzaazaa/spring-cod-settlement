import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { Truck } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useI18n } from '@/lib/i18n';
import { formatNumber, initials } from '@/lib/format';
import { formatMoney, formatPercent, moneySign, absMoney, ratioPercent, toChartNumber } from '@/lib/money';
import type { CourierPosition } from '@/lib/types';
import { EmptyState } from './ui/EmptyState';
import { Money } from './ui/Money';
import { Term } from './ui/Term';

type Health = 'settled' | 'overpaid' | 'onTrack' | 'behind' | 'none';

/** How a courier is doing: share of the expected cash it has reported. */
export function courierHealth(c: CourierPosition): { health: Health; pct: string | null } {
  const pct = ratioPercent(c.reportedCollections, c.expectedCollections);
  const sign = moneySign(c.balanceOwed);
  if (sign === 0) return { health: 'settled', pct };
  if (sign < 0) return { health: 'overpaid', pct };
  const n = Number(pct ?? 0);
  return { health: n >= 80 ? 'onTrack' : n > 0 ? 'behind' : 'none', pct };
}

const CHIP: Record<Health, string> = {
  settled: 'bg-signal/10 text-signal-ink ring-signal/25',
  onTrack: 'bg-signal/10 text-signal-ink ring-signal/25',
  overpaid: 'bg-warn/10 text-warn ring-warn/25',
  behind: 'bg-warn/10 text-warn ring-warn/25',
  none: 'bg-danger/10 text-danger ring-danger/25',
};
const BAR: Record<Health, string> = { settled: 'bg-signal', onTrack: 'bg-signal', overpaid: 'bg-signal', behind: 'bg-warn', none: 'bg-danger' };
const OWED: Record<Health, string> = { settled: 'text-faint', onTrack: 'text-fg', overpaid: 'text-warn', behind: 'text-warn', none: 'text-danger' };

/** One small status chip; the amount itself is shown once, in the "Still owed" column. */
export function BalanceStatus({ courier }: { courier: CourierPosition }) {
  const { t, lang } = useI18n();
  const { health } = courierHealth(courier);
  const label = health === 'overpaid'
    ? t('balances.overpaid', { amount: formatMoney(absMoney(courier.balanceOwed), lang) })
    : t(`balances.status.${health}`);
  return (
    <span data-testid="balance-status" data-health={health}
      className={clsx('inline-block whitespace-nowrap rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset', CHIP[health])}>
      {label}
    </span>
  );
}

export function CourierBalanceTable({ couriers }: { couriers: CourierPosition[] }) {
  const { t, lang } = useI18n();
  if (couriers.length === 0) return <EmptyState icon={<Truck className="h-4 w-4" />} title={t('balances.empty')} />;
  return (
    <div className="scrollbar-thin overflow-x-auto">
      <table className="w-full" data-testid="balance-table">
        <thead>
          <tr>
            <th className="th">{t('balances.courier')}</th>
            <th className="th text-right">{t('balances.delivered')}</th>
            <th className="th text-right"><Term k="expected">{t('balances.expected')}</Term></th>
            <th className="th text-right"><Term k="reported">{t('balances.reported')}</Term></th>
            <th className="th text-right"><Term k="fees">{t('balances.fees')}</Term></th>
            <th className="th text-right"><Term k="netRemitted">{t('balances.net')}</Term></th>
            <th className="th pr-4 text-right"><Term k="balanceOwed">{t('balances.owed')}</Term></th>
          </tr>
        </thead>
        <tbody>
          {couriers.map((c) => {
            const { health, pct } = courierHealth(c);
            return (
              <tr key={c.courierId} className="border-t border-line align-top hover:bg-subtle/50" data-testid="balance-row" data-code={c.code}>
                <td className="td">
                  <div className="flex items-center gap-2.5">
                    <span className="avatar h-8 w-8 text-[11px]">{initials(c.name)}</span>
                    <div className="min-w-0">
                      <Link to={`/orders?courierId=${c.courierId}`} className="focus-ring rounded font-medium hover:underline">{c.name}</Link>
                      <div className="mt-1 flex items-center gap-2">
                        <div className={clsx('h-1.5 w-24 overflow-hidden rounded-full', health === 'none' ? 'bg-danger/20' : 'bg-subtle')}
                          role="meter" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Number(pct ?? 0)} aria-label={t('balances.reportedShare')}>
                          <div className={clsx('h-full rounded-full', BAR[health])} style={{ width: `${Math.min(100, Number(pct ?? 0))}%` }} />
                        </div>
                        <span className="text-[11px] tabular-nums text-faint">{formatPercent(pct, lang)}</span>
                      </div>
                    </div>
                  </div>
                </td>
                <td className="td num text-muted">{formatNumber(c.deliveredOrders, lang)}</td>
                <td className="td num"><Money value={c.expectedCollections} unit={false} /></td>
                <td className="td num"><Money value={c.reportedCollections} unit={false} /></td>
                <td className="td num"><Money value={c.fees} unit={false} muted /></td>
                <td className="td num"><Money value={c.netRemitted} unit={false} /></td>
                <td className="td whitespace-normal pr-4 text-right">
                  <div className="ms-auto flex flex-col items-end gap-1">
                    <Money value={c.balanceOwed} className={clsx('font-semibold', OWED[health])} />
                    <BalanceStatus courier={c} />
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

interface ChartRow {
  name: string;
  expected: number;
  reported: number;
  expectedLabel: string;
  reportedLabel: string;
}

function ChartTooltip({ active, payload }: { active?: boolean; payload?: Array<{ payload: ChartRow }> }) {
  const { t } = useI18n();
  const row = payload?.[0]?.payload;
  if (!active || !row) return null;
  return (
    <div className="rounded-lg border border-line bg-panel px-3 py-2 text-[12px] shadow-lift">
      <p className="mb-1 font-medium">{row.name}</p>
      <p className="flex justify-between gap-4 text-muted"><span>{t('balances.expected')}</span><span className="tabular-nums text-fg">{row.expectedLabel}</span></p>
      <p className="flex justify-between gap-4 text-muted"><span>{t('balances.reported')}</span><span className="tabular-nums text-fg">{row.reportedLabel}</span></p>
    </div>
  );
}

/** Floats exist only for bar geometry; every label comes from the API string via formatMoney. */
export function BalanceChart({ couriers }: { couriers: CourierPosition[] }) {
  const { t, lang } = useI18n();
  const data: ChartRow[] = couriers.map((c) => ({
    name: c.name,
    expected: toChartNumber(c.expectedCollections),
    reported: toChartNumber(c.reportedCollections),
    expectedLabel: formatMoney(c.expectedCollections, lang),
    reportedLabel: formatMoney(c.reportedCollections, lang),
  }));
  return (
    <div className="h-[230px] w-full text-faint" data-testid="balance-chart" aria-label={t('balances.chart')}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} barGap={4} margin={{ top: 8, right: 4, left: 0, bottom: 0 }}>
          <CartesianGrid vertical={false} stroke="currentColor" strokeOpacity={0.18} />
          <XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fill: 'currentColor', fontSize: 11 }} />
          <YAxis tickLine={false} axisLine={false} width={44} tick={{ fill: 'currentColor', fontSize: 11 }}
            tickFormatter={(v: number) => new Intl.NumberFormat(lang === 'fr' ? 'fr-MA' : 'en-GB', { notation: 'compact' }).format(v)} />
          <Tooltip content={<ChartTooltip />} cursor={{ fill: 'currentColor', fillOpacity: 0.06 }} />
          <Bar dataKey="expected" name={t('balances.expected')} radius={[6, 6, 0, 0]} maxBarSize={36} className="text-accent/25" fill="currentColor" />
          <Bar dataKey="reported" name={t('balances.reported')} radius={[6, 6, 0, 0]} maxBarSize={36} className="text-signal" fill="currentColor" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
