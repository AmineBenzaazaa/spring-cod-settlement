import clsx from 'clsx';
import { useI18n, type MessageKey } from '@/lib/i18n';
import { tone } from '@/lib/status';

export function Dot({ value, pulse }: { value: string; pulse?: boolean }) {
  const s = tone(value);
  return (
    <span className="relative inline-flex h-2 w-2 shrink-0">
      {pulse && <span className={clsx('absolute inset-0 rounded-full animate-pulse-ring', s.dot)} />}
      <span className={clsx('relative inline-flex h-2 w-2 rounded-full', s.dot)} />
    </span>
  );
}

/** Pill for any enum value. `prefix` picks the dictionary namespace ("status", "outcome", "order"...). */
export function Badge({ value, prefix, pulse, className }: { value: string; prefix: string; pulse?: boolean; className?: string }) {
  const { t } = useI18n();
  const s = tone(value);
  return (
    <span
      data-testid={`badge-${prefix}`}
      data-value={value}
      className={clsx(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset',
        s.bg, s.text, s.ring, className,
      )}
    >
      <Dot value={value} pulse={pulse} />
      {t(`${prefix}.${value}` as MessageKey)}
    </span>
  );
}
