import { Fragment, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import clsx from 'clsx';
import { ChevronRight, Package, Search } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatDateTime, relativeTime } from '@/lib/format';
import { useCouriers, useOrders } from '@/lib/queries';
import { useDebounced } from '@/lib/useDebounced';
import type { Order, OrderStatus } from '@/lib/types';
import { Badge, Dot } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { Money } from '@/components/ui/Money';
import { PageHeader, TableSkeletonRows } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';

const STATUSES: OrderStatus[] = ['PENDING', 'SHIPPED', 'DELIVERED', 'RETURNED', 'CANCELLED'];

function Attempts({ order }: { order: Order }) {
  const { t, lang } = useI18n();
  if (order.attempts.length === 0) return <p className="text-[12.5px] text-muted">{t('orders.noAttempts')}</p>;
  return (
    <ol className="relative ml-1.5 border-l border-line" data-testid="attempts">
      {order.attempts.map((a, i) => (
        <li key={a.id} className="relative pb-3 pl-5 last:pb-0">
          <span className="absolute -left-[4.5px] top-1.5"><Dot value={a.outcome} /></span>
          <div className="flex items-baseline gap-3">
            <span className="text-[13px] font-medium">{i + 1}. {t(`attempt.${a.outcome}`)}</span>
            <time className="font-mono text-[11px] text-faint" dateTime={a.attemptedAt}>{formatDateTime(a.attemptedAt, lang)}</time>
          </div>
          {a.note && <p className="mt-0.5 text-[12px] text-muted">{a.note}</p>}
        </li>
      ))}
    </ol>
  );
}

export function OrdersPage() {
  const { t, lang } = useI18n();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState(params.get('q') ?? '');
  const q = useDebounced(search);
  const status = params.get('status') as OrderStatus | null;
  const courierId = params.get('courierId') ? Number(params.get('courierId')) : null;
  const page = Number(params.get('page') ?? 0);
  const [expanded, setExpanded] = useState<number | null>(null);
  const couriers = useCouriers();
  const orders = useOrders({ q, status: status ? [status] : undefined, courierId, page, size: 15 });

  const update = (next: Record<string, string | null>, resetPage = true) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) if (v == null || v === '') p.delete(k); else p.set(k, v);
    if (resetPage) p.delete('page');
    setParams(p, { replace: true });
  };

  return (
    <>
      <PageHeader title={t('orders.title')} subtitle={t('orders.subtitle')} />
      <section className="panel min-w-0">
        <div className="flex flex-wrap items-center gap-2 border-b border-line p-3">
          <div className="relative w-full sm:w-80">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-faint" />
            <input className="input h-8 pl-8 text-[13px]" placeholder={t('orders.search')} aria-label={t('orders.search')} value={search}
              onChange={(e) => { setSearch(e.target.value); update({ q: e.target.value }); }} data-testid="orders-search" />
          </div>
          <select className="input h-8 w-auto text-[13px]" value={status ?? ''} onChange={(e) => update({ status: e.target.value })} aria-label={t('orders.status')}>
            <option value="">{t('orders.allStatuses')}</option>
            {STATUSES.map((s) => <option key={s} value={s}>{t(`order.${s}`)}</option>)}
          </select>
          <select className="input h-8 w-auto text-[13px]" value={courierId ?? ''} onChange={(e) => update({ courierId: e.target.value })} aria-label={t('orders.courier')}>
            <option value="">{t('orders.allCouriers')}</option>
            {couriers.data?.content.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        {orders.isError ? <ErrorState error={orders.error} onRetry={() => orders.refetch()} />
          : orders.data?.totalElements === 0 ? (
            <EmptyState icon={<Package className="h-4 w-4" />} title={t('orders.empty')} hint={t('orders.emptyHint')} />
          ) : (
            <div className="scrollbar-thin overflow-x-auto">
              <table className={clsx('w-full transition-opacity', orders.isPlaceholderData && 'opacity-60')} data-testid="orders-table">
                <thead>
                  <tr>
                    <th className="th w-8" />
                    <th className="th">{t('orders.tracking')}</th>
                    <th className="th">{t('orders.customer')}</th>
                    <th className="th">{t('orders.city')}</th>
                    <th className="th">{t('orders.courier')}</th>
                    <th className="th text-right">{t('orders.amount')}</th>
                    <th className="th">{t('orders.status')}</th>
                    <th className="th text-right">{t('orders.attempts')}</th>
                    <th className="th">{t('orders.created')}</th>
                  </tr>
                </thead>
                <tbody>
                  {!orders.data ? <TableSkeletonRows cols={9} /> : orders.data.content.map((o) => (
                    <Fragment key={o.id}>
                      <tr data-testid="order-row" data-tracking={o.trackingNumber} tabIndex={0} aria-expanded={expanded === o.id}
                        onClick={() => setExpanded(expanded === o.id ? null : o.id)}
                        onKeyDown={(e) => e.key === 'Enter' && setExpanded(expanded === o.id ? null : o.id)}
                        className="focus-ring cursor-pointer border-t border-line hover:bg-subtle/60">
                        <td className="td pr-0 text-faint"><ChevronRight className={clsx('h-3.5 w-3.5 transition-transform', expanded === o.id && 'rotate-90')} /></td>
                        <td className="td font-mono text-[12.5px] font-medium">{o.trackingNumber}</td>
                        <td className="td">
                          <p>{o.customerName}</p>
                          {o.customerPhone && <p className="font-mono text-[11px] text-faint">{o.customerPhone}</p>}
                        </td>
                        <td className="td text-muted">{o.city ?? '—'}</td>
                        <td className="td text-muted">{o.courierName}</td>
                        <td className="td num"><Money value={o.amountDue} /></td>
                        <td className="td"><Badge value={o.status} prefix="order" /></td>
                        <td className="td num text-muted">{o.attempts.length}</td>
                        <td className="td text-muted" title={formatDateTime(o.createdAt, lang)}>{relativeTime(o.createdAt, lang)}</td>
                      </tr>
                      {expanded === o.id && (
                        <tr className="bg-subtle/40">
                          <td />
                          <td colSpan={8} className="px-3 py-3">
                            <p className="label mb-2">{t('orders.attemptsTitle')}</p>
                            <Attempts order={o} />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        {orders.data && <Pager page={orders.data} onPage={(p) => update({ page: String(p) }, false)} fetching={orders.isPlaceholderData} />}
      </section>
    </>
  );
}
