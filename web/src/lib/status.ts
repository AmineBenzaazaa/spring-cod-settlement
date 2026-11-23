export interface Tone {
  dot: string;
  text: string;
  bg: string;
  ring: string;
}

// Brand semantic tones (tokens from brand/tokens.css): green signal = paid / matched / done,
// warn = needs a look, danger = money missing, info = in progress or unknown, neutral = waiting.
const neutral: Tone = { dot: 'bg-faint', text: 'text-muted', bg: 'bg-subtle', ring: 'ring-line' };
const info: Tone = { dot: 'bg-info', text: 'text-info', bg: 'bg-info/10', ring: 'ring-info/25' };
const warn: Tone = { dot: 'bg-warn', text: 'text-warn', bg: 'bg-warn/10', ring: 'ring-warn/25' };
const signal: Tone = { dot: 'bg-signal', text: 'text-signal-ink', bg: 'bg-signal/10', ring: 'ring-signal/25' };
const danger: Tone = { dot: 'bg-danger', text: 'text-danger', bg: 'bg-danger/10', ring: 'ring-danger/25' };
const zinc = neutral, sky = info, amber = warn, emerald = signal, rose = danger, violet = info;

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
  MATCHED: 'border-l-signal/70',
  AMOUNT_MISMATCH: 'border-l-warn bg-warn/[0.04]',
  UNKNOWN_TRACKING: 'border-l-info bg-info/[0.04]',
  MISSING_FROM_STATEMENT: 'border-l-danger bg-danger/[0.04]',
};
