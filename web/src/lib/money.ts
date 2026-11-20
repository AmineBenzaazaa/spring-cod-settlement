import type { Lang } from './i18n';

/**
 * Money arrives from the API as decimal strings ("349.90", "-50.00"): Java BigDecimal serialized
 * as text, so JavaScript never sees a float. Everything here works on integer cents held in a
 * BigInt. `0.1 + 0.2` problems cannot happen, and amounts above 2^53 stay exact.
 */
export type Money = string;

const MONEY_RE = /^\s*([+-])?(\d+)(?:\.(\d*))?\s*$/;

/** "12.345" -> 1235n (half-up, like the backend's HALF_UP). Throws on anything that is not a decimal. */
export function toCents(value: Money): bigint {
  const m = MONEY_RE.exec(value);
  if (!m) throw new Error(`Not a decimal amount: "${value}"`);
  const [, sign, int, frac = ''] = m;
  const padded = (frac + '000').slice(0, 3);
  let cents = BigInt(int) * 100n + BigInt(padded.slice(0, 2));
  if (Number(padded[2]) >= 5) cents += 1n;
  return sign === '-' ? -cents : cents;
}

/** 123450n -> "1234.50" */
export function fromCents(cents: bigint): Money {
  const negative = cents < 0n;
  const abs = negative ? -cents : cents;
  const int = abs / 100n;
  const frac = (abs % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}${int}.${frac}`;
}

export function moneySign(value: Money): -1 | 0 | 1 {
  const c = toCents(value);
  return c === 0n ? 0 : c < 0n ? -1 : 1;
}

export function absMoney(value: Money): Money {
  const c = toCents(value);
  return fromCents(c < 0n ? -c : c);
}

export function sumMoney(values: Money[]): Money {
  return fromCents(values.reduce((acc, v) => acc + toCents(v), 0n));
}

const locales: Record<Lang, string> = { en: 'en-GB', fr: 'fr-MA' };
const separatorCache = new Map<Lang, { group: string; decimal: string }>();

/** Grouping and decimal symbols from Intl, applied to the digits ourselves (no Number conversion). */
function separators(lang: Lang) {
  let s = separatorCache.get(lang);
  if (!s) {
    const parts = new Intl.NumberFormat(locales[lang]).formatToParts(11111.5);
    s = {
      group: parts.find((p) => p.type === 'group')?.value ?? ',',
      decimal: parts.find((p) => p.type === 'decimal')?.value ?? '.',
    };
    separatorCache.set(lang, s);
  }
  return s;
}

export interface FormatOptions {
  /** append " MAD" (default true) */
  currency?: boolean;
  /** prefix positive amounts with "+" (for differences) */
  signed?: boolean;
}

/** "1234.5" -> "1,234.50 MAD" (en) / "1.234,50 MAD" (fr). null/undefined/invalid -> "—". */
export function formatMoney(value: Money | null | undefined, lang: Lang = 'en', opts: FormatOptions = {}): string {
  if (value == null) return '—';
  let cents: bigint;
  try {
    cents = toCents(value);
  } catch {
    return '—';
  }
  const { group, decimal } = separators(lang);
  const [int, frac] = fromCents(cents < 0n ? -cents : cents).split('.');
  const grouped = int!.replace(/\B(?=(\d{3})+(?!\d))/g, group);
  const sign = cents < 0n ? '−' : opts.signed && cents > 0n ? '+' : '';
  const n = `${sign}${grouped}${decimal}${frac}`;
  return opts.currency === false ? n : `${n} MAD`;
}

/** What a user may type in an amount field: up to 10 integer digits and 2 decimals, "," or ".". */
export function isValidMoneyInput(input: string): boolean {
  return /^\d{1,10}([.,]\d{1,2})?$/.test(input.trim());
}

/** "12,5" -> "12.50", ready to send to the API as a string. */
export function normalizeMoneyInput(input: string): Money {
  return fromCents(toCents(input.trim().replace(',', '.')));
}

/**
 * The only place a float is created: chart geometry (bar heights) needs numbers. Labels and
 * tooltips are still rendered from the original strings with formatMoney.
 */
export function toChartNumber(value: Money): number {
  return Number(toCents(value)) / 100;
}

/** part / whole as a percentage with one decimal, computed on cents ("88.4"). null when whole is 0. */
export function ratioPercent(part: Money, whole: Money): string | null {
  const w = toCents(whole);
  if (w === 0n) return null;
  const tenths = (toCents(part) * 1000n + w / 2n) / w; // rounded half-up for positive values
  const sign = tenths < 0n ? '-' : '';
  const abs = tenths < 0n ? -tenths : tenths;
  return `${sign}${abs / 10n}.${abs % 10n}`;
}
