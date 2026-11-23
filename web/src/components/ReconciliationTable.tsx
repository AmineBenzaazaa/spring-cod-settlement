import { useMemo, useState } from 'react';
import clsx from 'clsx';
import { Gavel, Search, SearchX } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatNumber } from '@/lib/format';
import { outcomeRow, tone } from '@/lib/status';
import { OUTCOMES, type Outcome, type ReconciliationResult, type ReconciliationSummary } from '@/lib/types';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { Money } from './ui/Money';

export type OutcomeFilter = Outcome | 'ALL' | 'ISSUES';

export function filterResults(results: ReconciliationResult[], filter: OutcomeFilter, search: string): ReconciliationResult[] {
  const q = search.trim().toUpperCase();
  return results.filter((r) =>
    (filter === 'ALL' || (filter === 'ISSUES' ? r.outcome !== 'MATCHED' : r.outcome === filter))
    && (!q || r.trackingNumber.toUpperCase().includes(q)));
}

/** Segmented bar: one segment per outcome, width proportional to its count. */
export function OutcomeBar({ counts }: { counts: Record<Outcome, number> }) {
  const total = OUTCOMES.reduce((n, o) => n + (counts[o] ?? 0), 0);
  if (total === 0) return null;
  return (
    <div className="flex h-1.5 w-full gap-0.5 overflow-hidden rounded-full" aria-hidden>
      {OUTCOMES.filter((o) => counts[o]).map((o) => (
        <div key={o} className={tone(o).dot} style={{ width: `${(counts[o] / total) * 100}%` }} />
      ))}
    </div>
  );
}

export function ReconciliationTable({ results, summary, onOpenDispute }: {
  results: ReconciliationResult[];
  summary: ReconciliationSummary;
  onOpenDispute: (result: ReconciliationResult) => void;
}) {
  const { t, lang } = useI18n();
  const [filter, setFilter] = useState<OutcomeFilter>('ALL');
  const [search, setSearch] = useState('');
  const rows = useMemo(() => filterResults(results, filter, search), [results, filter, search]);
  const issues = results.length - (summary.counts.MATCHED ?? 0);

  const chip = (value: OutcomeFilter, label: string, count: number) => (
    <button key={value} type="button" onClick={() => setFilter(value)} data-testid={`filter-${value}`} aria-pressed={filter === value}
      className={clsx('focus-ring inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-[12px] font-medium transition-colors',
        filter === value ? 'border-fg/20 bg-fg text-bg' : 'border-line bg-panel text-muted hover:bg-subtle hover:text-fg')}>
      {value !== 'ALL' && value !== 'ISSUES' && <span className={clsx('h-1.5 w-1.5 rounded-full', tone(value).dot)} />}
      {label}
      <span className={clsx('font-mono text-[11px]', filter === value ? 'text-bg/70' : 'text-faint')} data-testid={`count-${value}`}>{formatNumber(count, lang)}</span>
    </button>
  );

  return (
    <section className="panel min-w-0" aria-label={t('recon.outcome')}>
      <div className="space-y-3 border-b border-line p-3">
        <OutcomeBar counts={summary.counts} />
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex flex-wrap gap-1.5">
            {chip('ALL', t('outcome.all'), results.length)}
            {chip('ISSUES', t('outcome.issues'), issues)}
            {OUTCOMES.map((o) => chip(o, t(`outcome.${o}`), summary.counts[o] ?? 0))}
          </div>
          <div className="relative ml-auto w-full sm:w-56">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-faint" />
            <input className="input h-8 pl-8 font-mono text-[12.5px]" placeholder={t('recon.search')} value={search}
              onChange={(e) => setSearch(e.target.value)} aria-label={t('recon.search')} />
          </div>
        </div>
      </div>
      {rows.length === 0 ? (
        <EmptyState icon={<SearchX className="h-4 w-4" />} title={t('recon.empty')} />
      ) : (
        <div className="scrollbar-thin overflow-x-auto">
          <table className="w-full" data-testid="recon-table">
            <thead>
              <tr>
                <th className="th w-12">{t('recon.row')}</th>
                <th className="th">{t('recon.tracking')}</th>
                <th className="th">{t('recon.outcome')}</th>
                <th className="th text-right">{t('recon.expected')}</th>
                <th className="th text-right">{t('recon.collected')}</th>
                <th className="th text-right">{t('recon.fees')}</th>
                <th className="th text-right">{t('recon.difference')}</th>
                <th className="th text-right"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} data-testid="recon-row" data-outcome={r.outcome} data-tracking={r.trackingNumber}
                  className={clsx('border-l-2 border-t border-t-line', outcomeRow[r.outcome])}>
                  <td className="td font-mono text-faint">{r.line?.rowNumber ?? '—'}</td>
                  <td className="td font-mono text-[12.5px] font-medium">{r.trackingNumber}</td>
                  <td className="td"><Badge value={r.outcome} prefix="outcome" /></td>
                  <td className="td num"><Money value={r.expectedAmount} unit={false} /></td>
                  <td className="td num">{r.line ? <Money value={r.collectedAmount} unit={false} /> : <span className="text-faint">—</span>}</td>
                  <td className="td num">{r.line ? <Money value={r.line.fee} unit={false} muted /> : <span className="text-faint">—</span>}</td>
                  <td className="td num font-medium"><Money value={r.difference} diff unit={false} /></td>
                  <td className="td text-right">
                    {r.outcome === 'MATCHED' ? null : r.hasDispute ? (
                      <span className="inline-flex items-center gap-1 text-[12px] font-medium text-warn" data-testid="disputed">
                        <Gavel className="h-3.5 w-3.5" />{t('recon.disputed')}
                      </span>
                    ) : (
                      <Button size="sm" onClick={() => onOpenDispute(r)} data-testid="open-dispute">
                        <Gavel className="h-3.5 w-3.5" />{t('recon.openDispute')}
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
