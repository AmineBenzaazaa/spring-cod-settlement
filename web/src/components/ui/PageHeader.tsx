import type { ReactNode } from 'react';

export function PageHeader({ title, subtitle, actions, eyebrow }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode; eyebrow?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        {eyebrow}
        <h1 className="text-[19px] font-semibold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-0.5 text-[13px] text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function TableSkeletonRows({ cols, rows = 6 }: { cols: number; rows?: number }) {
  return (
    <>
      {Array.from({ length: rows }, (_, r) => (
        <tr key={r} className="border-t border-line">
          {Array.from({ length: cols }, (_, c) => (
            <td key={c} className="td"><div className="h-3.5 animate-pulse rounded bg-subtle" style={{ width: `${40 + ((r * 7 + c * 13) % 50)}%` }} /></td>
          ))}
        </tr>
      ))}
    </>
  );
}
