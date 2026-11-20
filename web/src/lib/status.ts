export interface Tone {
  dot: string;
  text: string;
  bg: string;
  ring: string;
}

const zinc: Tone = { dot: 'bg-zinc-400', text: 'text-zinc-600 dark:text-zinc-300', bg: 'bg-zinc-500/10', ring: 'ring-zinc-500/20' };
const sky: Tone = { dot: 'bg-sky-500', text: 'text-sky-700 dark:text-sky-300', bg: 'bg-sky-500/10', ring: 'ring-sky-500/25' };
const amber: Tone = { dot: 'bg-amber-500', text: 'text-amber-700 dark:text-amber-300', bg: 'bg-amber-500/10', ring: 'ring-amber-500/25' };
const emerald: Tone = { dot: 'bg-emerald-500', text: 'text-emerald-700 dark:text-emerald-300', bg: 'bg-emerald-500/10', ring: 'ring-emerald-500/25' };
const rose: Tone = { dot: 'bg-rose-500', text: 'text-rose-700 dark:text-rose-300', bg: 'bg-rose-500/10', ring: 'ring-rose-500/25' };
const violet: Tone = { dot: 'bg-violet-500', text: 'text-violet-700 dark:text-violet-300', bg: 'bg-violet-500/10', ring: 'ring-violet-500/25' };

/** One palette for every enum the UI shows, so a colour always means the same thing. */
const tones: Record<string, Tone> = {
  // statements
  UPLOADED: zinc, PROCESSING: sky, RECONCILED: emerald, FAILED: rose,
  // reconciliation outcomes
  MATCHED: emerald, AMOUNT_MISMATCH: amber, UNKNOWN_TRACKING: violet, MISSING_FROM_STATEMENT: rose,
  // disputes
  OPEN: amber, RESOLVED: emerald, REJECTED: zinc,
  // orders and attempts
  PENDING: zinc, SHIPPED: sky, DELIVERED: emerald, RETURNED: violet, CANCELLED: zinc,
  NO_ANSWER: amber, REFUSED: rose, RESCHEDULED: sky,
};

export function tone(value: string): Tone {
  return tones[value] ?? zinc;
}

/** Row tint for reconciliation lines (left border + soft background). */
export const outcomeRow: Record<string, string> = {
  MATCHED: 'border-l-emerald-500/70',
  AMOUNT_MISMATCH: 'border-l-amber-500 bg-amber-500/[0.04]',
  UNKNOWN_TRACKING: 'border-l-violet-500 bg-violet-500/[0.04]',
  MISSING_FROM_STATEMENT: 'border-l-rose-500 bg-rose-500/[0.04]',
};
