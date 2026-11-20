import clsx from 'clsx';
import { useI18n } from '@/lib/i18n';
import { formatMoney, moneySign, type Money as MoneyValue } from '@/lib/money';

/**
 * Renders an API money string. `diff` colours the sign (short payment red, over-payment amber)
 * and shows "+" on positive values.
 */
export function Money({ value, diff, muted, className, unit = true }: {
  value: MoneyValue | null | undefined; diff?: boolean; muted?: boolean; className?: string; unit?: boolean;
}) {
  const { lang } = useI18n();
  const sign = value != null && diff ? moneySign(value) : 0;
  return (
    <span
      className={clsx(
        'font-mono tabular-nums',
        diff && sign < 0 && 'text-rose-600 dark:text-rose-400',
        diff && sign > 0 && 'text-amber-600 dark:text-amber-400',
        diff && sign === 0 && 'text-faint',
        muted && 'text-muted',
        className,
      )}
    >
      {formatMoney(value, lang, { currency: false, signed: diff })}
      {unit && value != null && <span className="ml-1 font-sans text-[0.78em] font-medium text-faint">MAD</span>}
    </span>
  );
}
