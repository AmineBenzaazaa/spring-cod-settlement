import type { Lang } from './i18n';

const locales: Record<Lang, string> = { en: 'en-GB', fr: 'fr-MA' };

export function formatNumber(n: number, lang: Lang = 'en'): string {
  return new Intl.NumberFormat(locales[lang]).format(n);
}

export function formatDateTime(iso: string | null | undefined, lang: Lang = 'en'): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString(locales[lang], {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
  });
}

/**
 * A java.time.LocalDate ("2026-10-01") is a calendar day, not an instant: format it in UTC so
 * a browser west of Greenwich does not show 30 September.
 */
export function formatLocalDate(day: string, lang: Lang = 'en', withYear = true): string {
  const [y, m, d] = day.split('-').map(Number);
  return new Date(Date.UTC(y!, m! - 1, d!)).toLocaleDateString(locales[lang], {
    day: 'numeric', month: 'short', ...(withYear ? { year: 'numeric' } : {}), timeZone: 'UTC',
  });
}

export function formatPeriod(start: string, end: string, lang: Lang = 'en'): string {
  const sameYear = start.slice(0, 4) === end.slice(0, 4);
  return `${formatLocalDate(start, lang, !sameYear)} – ${formatLocalDate(end, lang)}`;
}

export function relativeTime(iso: string, lang: Lang = 'en', now = Date.now()): string {
  const diff = Math.round((new Date(iso).getTime() - now) / 1000);
  const rtf = new Intl.RelativeTimeFormat(locales[lang], { numeric: 'auto', style: 'short' });
  const abs = Math.abs(diff);
  if (abs < 45) return rtf.format(diff, 'second');
  if (abs < 2700) return rtf.format(Math.round(diff / 60), 'minute');
  if (abs < 64800) return rtf.format(Math.round(diff / 3600), 'hour');
  return rtf.format(Math.round(diff / 86400), 'day');
}

/** "medina" -> "ME", "Speedex Colis" -> "SC" */
export function initials(name: string): string {
  const parts = name.split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0]!.slice(0, 2).toUpperCase();
  return parts.slice(0, 2).map((p) => p[0]!.toUpperCase()).join('');
}

/** Default period for a new upload: the previous calendar month. */
export function previousMonth(today = new Date()): { start: string; end: string } {
  const first = new Date(Date.UTC(today.getFullYear(), today.getMonth() - 1, 1));
  const last = new Date(Date.UTC(today.getFullYear(), today.getMonth(), 0));
  const iso = (d: Date) => d.toISOString().slice(0, 10);
  return { start: iso(first), end: iso(last) };
}
