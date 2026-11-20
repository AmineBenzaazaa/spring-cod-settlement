/** Same tile as the Parcel Ops logo (accent square, rounded 8), with a ledger-and-check glyph. */
export function Logo({ size = 26 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden className="shrink-0">
      <rect width="32" height="32" rx="8" className="fill-accent" />
      <path d="M9 11h14M9 16h9M9 21h6" fill="none" className="stroke-accent-fg" strokeWidth="2" strokeLinecap="round" />
      <path d="m18.5 21 2 2 4-4.5" fill="none" className="stroke-accent-fg" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
