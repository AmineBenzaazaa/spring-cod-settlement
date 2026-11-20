import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, Truck } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatDateTime, initials } from '@/lib/format';
import { useCourierPage, useOverview } from '@/lib/queries';
import { useDebounced } from '@/lib/useDebounced';
import { OwedPill } from '@/components/CourierBalances';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { Money } from '@/components/ui/Money';
import { PageHeader, TableSkeletonRows } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';

export function CouriersPage() {
  const { t, lang } = useI18n();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const q = useDebounced(search);
  const couriers = useCourierPage(q, page);
  const overview = useOverview();
  const position = (id: number) => overview.data?.couriers.find((c) => c.courierId === id);

  return (
    <>
      <PageHeader title={t('couriers.title')} subtitle={t('couriers.subtitle')} />
      <section className="panel min-w-0">
        <div className="border-b border-line p-3">
          <div className="relative w-full sm:w-72">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-faint" />
            <input className="input h-8 pl-8 text-[13px]" placeholder={t('couriers.search')} aria-label={t('couriers.search')} value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
          </div>
        </div>
        {couriers.isError ? <ErrorState error={couriers.error} onRetry={() => couriers.refetch()} />
          : couriers.data?.totalElements === 0 ? <EmptyState icon={<Truck className="h-4 w-4" />} title={t('couriers.empty')} /> : (
            <div className="scrollbar-thin overflow-x-auto">
              <table className="w-full" data-testid="couriers-table">
                <thead>
                  <tr>
                    <th className="th">{t('couriers.name')}</th>
                    <th className="th">{t('couriers.code')}</th>
                    <th className="th">{t('couriers.phone')}</th>
                    <th className="th">{t('history.status')}</th>
                    <th className="th text-right">{t('balances.delivered')}</th>
                    <th className="th text-right">{t('balances.owed')}</th>
                    <th className="th">{t('couriers.since')}</th>
                    <th className="th" />
                  </tr>
                </thead>
                <tbody>
                  {!couriers.data ? <TableSkeletonRows cols={8} rows={3} /> : couriers.data.content.map((c) => {
                    const p = position(c.id);
                    return (
                      <tr key={c.id} className="border-t border-line hover:bg-subtle/50" data-testid="courier-row">
                        <td className="td">
                          <span className="flex items-center gap-2.5">
                            <span className="inline-flex h-7 w-7 items-center justify-center rounded-lg bg-accent/10 text-[10px] font-semibold text-accent">{initials(c.name)}</span>
                            <span className="font-medium">{c.name}</span>
                          </span>
                        </td>
                        <td className="td font-mono text-muted">{c.code}</td>
                        <td className="td font-mono text-[12px] text-muted">{c.contactPhone ?? '—'}</td>
                        <td className="td">
                          <span className={`inline-flex items-center gap-1.5 text-[12px] ${c.active ? 'text-emerald-700 dark:text-emerald-300' : 'text-faint'}`}>
                            <span className={`h-1.5 w-1.5 rounded-full ${c.active ? 'bg-emerald-500' : 'bg-zinc-400'}`} />
                            {c.active ? t('couriers.active') : t('couriers.inactive')}
                          </span>
                        </td>
                        <td className="td num text-muted">{p?.deliveredOrders ?? '—'}</td>
                        <td className="td text-right">
                          {p ? <div className="flex flex-col items-end gap-1"><Money value={p.balanceOwed} className="font-medium" /><OwedPill courier={p} /></div> : '—'}
                        </td>
                        <td className="td text-muted">{formatDateTime(c.createdAt, lang).split(',')[0]}</td>
                        <td className="td text-right">
                          <Link to={`/orders?courierId=${c.id}`} className="text-[12px] font-medium text-accent hover:underline">{t('couriers.viewOrders')} →</Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        {couriers.data && <Pager page={couriers.data} onPage={setPage} fetching={couriers.isPlaceholderData} />}
      </section>
    </>
  );
}
