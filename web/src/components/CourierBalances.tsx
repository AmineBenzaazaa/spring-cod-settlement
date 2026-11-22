import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { Truck } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useI18n } from '@/lib/i18n';
import { formatNumber, initials } from '@/lib/format';
import { formatMoney, moneySign, absMoney, ratioPercent, toChartNumber } from '@/lib/money';
import type { CourierPosition } from '@/lib/types';
import { EmptyState } from './ui/EmptyState';
import { Money } from './ui/Money';

export function OwedPill({ courier }: { courier: CourierPosition }) {
  const { t, lang } = useI18n();
  const sign = moneySign(courier.balanceOwed);
  if (sign === 0) {
    return <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[11px] font-medium text-emerald-700 ring-1 ring-inset ring-emerald-500/25 dark:text-emerald-300">{t('balances.settled')}</span>;
  }
  return (
    <span data-testid="owed-pill" className={clsx('inline-block whitespace-nowrap rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset',
      sign > 0 ? 'bg-rose-500/10 text-rose-700 ring-rose-500/25 dark:text-rose-300' : 'bg-amber-500/10 text-amber-700 ring-amber-500/25 dark:text-amber-300')}>
      {sign > 0
        ? t('balances.owes', { name: courier.name, amount: formatMoney(courier.balanceOwed, lang) })
        : t('balances.overpaid', { amount: formatMoney(absMoney(courier.balanceOwed), lang) })}
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
            <th className="th text-right">{t('balances.expected')}</th>
            <th className="th text-right">{t('balances.reported')}</th>
            <th className="th text-right">{t('balances.fees')}</th>
            <th className="th text-right">{t('balances.net')}</th>
            <th className="th pr-4 text-right">{t('balances.owed')}</th>
          </tr>
        </thead>
        <tbody>
          {couriers.map((c) => {
            const pct = ratioPercent(c.reportedCollections, c.expectedCollections);
            return (
              <tr key={c.courierId} className="border-t border-line align-top hover:bg-subtle/50" data-testid="balance-row" data-code={c.code}>
                <td className="td">
                  <div className="flex items-center gap-2.5">
                    <span className="inline-flex h-7 w-7 items-center justify-center rounded-lg bg-accent/10 text-[10px] font-semibold text-accent">{initials(c.name)}</span>
                    <div className="min-w-0">
                      <Link to={`/orders?courierId=${c.courierId}`} className="focus-ring rounded font-medium hover:underline">{c.name}</Link>
                      <div className="mt-1 flex items-center gap-2">
                        <div className="h-1 w-24 overflow-hidden rounded-full bg-subtle" title={pct ? `${pct} %` : undefined}>
                          <div className="h-full rounded-full bg-accent" style={{ width: `${Math.min(100, Number(pct ?? 0))}%` }} />
                        </div>
                        <span className="font-mono text-[11px] text-faint">{pct ? `${pct}%` : '—'}</span>
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
                  <div className="ml-auto flex max-w-[290px] flex-col items-end gap-1">
                    <Money value={c.balanceOwed} className="font-semibold" />
                    <OwedPill courier={c} />
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
      <p className="flex justify-between gap-4 text-muted"><span>{t('balances.expected')}</span><span className="font-mono text-fg">{row.expectedLabel}</span></p>
      <p className="flex justify-between gap-4 text-muted"><span>{t('balances.reported')}</span><span className="font-mono text-fg">{row.reportedLabel}</span></p>
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
          <Bar dataKey="expected" name={t('balances.expected')} radius={[4, 4, 0, 0]} maxBarSize={34} className="text-chart-2" fill="currentColor" />
          <Bar dataKey="reported" name={t('balances.reported')} radius={[4, 4, 0, 0]} maxBarSize={34} className="text-accent" fill="currentColor" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
