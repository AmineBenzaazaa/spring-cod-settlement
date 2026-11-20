import { AlertTriangle, RotateCw, SearchX } from 'lucide-react';
import { ApiError } from '@/lib/api';
import { useI18n } from '@/lib/i18n';
import { Button } from './Button';

/**
 * Shows an RFC 7807 ProblemDetail the way a person wants to read it: the title, the detail
 * sentence, and each field error, plus the HTTP status for whoever files the bug report.
 */
export function ProblemAlert({ error, className }: { error: unknown; className?: string }) {
  const { t } = useI18n();
  const problem = error instanceof ApiError ? error : null;
  const fields = problem ? Object.entries(problem.fieldErrors) : [];
  return (
    <div role="alert" data-testid="problem" className={`rounded-lg border border-rose-500/25 bg-rose-500/[0.06] px-3.5 py-3 text-[13px] ${className ?? ''}`}>
      <div className="flex items-start gap-2.5">
        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600 dark:text-rose-400" />
        <div className="min-w-0 space-y-1">
          <p className="font-medium text-rose-800 dark:text-rose-200">
            {problem?.title ?? t('error.title')}
            {problem && <span className="ml-2 rounded bg-rose-500/10 px-1.5 py-px font-mono text-[11px] font-normal">{problem.status}</span>}
          </p>
          <p className="text-rose-700/90 dark:text-rose-300/90">
            {problem ? problem.detail ?? problem.message : error instanceof TypeError ? t('error.network') : String((error as Error)?.message ?? error)}
          </p>
          {fields.length > 0 && (
            <ul className="mt-1.5 space-y-0.5 text-[12.5px] text-rose-700/90 dark:text-rose-300/90">
              {fields.flatMap(([field, msgs]) => msgs.map((m, i) => (
                <li key={`${field}-${i}`}><span className="font-mono text-[11.5px]">{field}</span>: {m}</li>
              )))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { t } = useI18n();
  if (error instanceof ApiError && error.status === 404) {
    return (
      <div className="flex flex-col items-center px-6 py-12 text-center" data-testid="not-found">
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl border border-line bg-subtle text-faint"><SearchX className="h-4 w-4" /></div>
        <p className="text-sm font-medium">{t('error.notFound')}</p>
        <p className="mt-1 max-w-xs text-[13px] text-muted">{t('error.notFoundHint')}</p>
      </div>
    );
  }
  return (
    <div className="space-y-3 p-4">
      <ProblemAlert error={error} />
      {onRetry && <Button size="sm" onClick={onRetry}><RotateCw className="h-3.5 w-3.5" />{t('error.retry')}</Button>}
    </div>
  );
}
