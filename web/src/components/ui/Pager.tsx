import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useI18n } from '@/lib/i18n';
import { formatNumber } from '@/lib/format';
import type { Page } from '@/lib/types';
import { IconButton } from './Button';

export function Pager({ page, onPage, fetching }: { page: Page<unknown>; onPage: (p: number) => void; fetching?: boolean }) {
  const { t, lang } = useI18n();
  if (page.totalElements === 0) return null;
  const from = page.page * page.size + 1;
  const to = Math.min(page.totalElements, from + page.content.length - 1);
  return (
    <div className="flex items-center justify-between border-t border-line px-3 py-2 text-[12px] text-muted" data-testid="pager">
      <span className={fetching ? 'opacity-60' : ''}>
        {t('pager.summary', { from: formatNumber(from, lang), to: formatNumber(to, lang), total: formatNumber(page.totalElements, lang) })}
      </span>
      <div className="flex items-center gap-0.5">
        <IconButton aria-label={t('pager.prev')} title={t('pager.prev')} disabled={page.page === 0} onClick={() => onPage(page.page - 1)}
          className="disabled:pointer-events-none disabled:opacity-40"><ChevronLeft className="h-4 w-4" /></IconButton>
        <span className="min-w-12 text-center tabular-nums">{page.page + 1}/{Math.max(page.totalPages, 1)}</span>
        <IconButton aria-label={t('pager.next')} title={t('pager.next')} disabled={page.page + 1 >= page.totalPages} onClick={() => onPage(page.page + 1)}
          className="disabled:pointer-events-none disabled:opacity-40"><ChevronRight className="h-4 w-4" /></IconButton>
      </div>
    </div>
  );
}
