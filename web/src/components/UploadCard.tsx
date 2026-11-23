import { useRef, useState, type DragEvent, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { CheckCircle2, Copy, FileSpreadsheet, UploadCloud, X } from 'lucide-react';
import { toast } from 'sonner';
import { useI18n } from '@/lib/i18n';
import { previousMonth } from '@/lib/format';
import { useCouriers, useUpload } from '@/lib/queries';
import type { StatementUploadResponse } from '@/lib/types';
import { Button } from './ui/Button';
import { ProblemAlert } from './ui/ErrorState';

function humanSize(bytes: number) {
  return bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KB`;
}

export function UploadCard({ onUploaded }: { onUploaded?: (res: StatementUploadResponse) => void }) {
  const { t } = useI18n();
  const couriers = useCouriers();
  const upload = useUpload();
  const inputRef = useRef<HTMLInputElement>(null);
  const period = previousMonth();
  const [courierId, setCourierId] = useState('');
  const [periodStart, setPeriodStart] = useState(period.start);
  const [periodEnd, setPeriodEnd] = useState(period.end);
  const [file, setFile] = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const [result, setResult] = useState<StatementUploadResponse | null>(null);

  const pick = (f: File | undefined | null) => {
    setResult(null);
    upload.reset();
    if (!f) return;
    if (!f.name.toLowerCase().endsWith('.csv')) {
      setLocalError(t('upload.onlyCsv'));
      setFile(null);
      return;
    }
    setLocalError(null);
    setFile(f);
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragging(false);
    pick(e.dataTransfer.files?.[0]);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!file) {
      setLocalError(t('upload.chooseFile'));
      return;
    }
    setResult(null);
    try {
      const res = await upload.mutateAsync({ file, courierId: Number(courierId), periodStart, periodEnd });
      setResult(res);
      if (!res.duplicate) {
        toast.success(t('upload.accepted', { id: res.statement.id }));
        setFile(null);
        if (inputRef.current) inputRef.current.value = '';
      }
      onUploaded?.(res);
    } catch {
      /* rendered below from upload.error */
    }
  };

  const activeCouriers = couriers.data?.content.filter((c) => c.active) ?? [];

  return (
    <form onSubmit={submit} className="panel flex flex-col" data-testid="upload-form" noValidate>
      <div className="border-b border-line px-4 py-3">
        <h2 className="text-[14px] font-semibold">{t('upload.title')}</h2>
        <p className="mt-0.5 text-[12px] text-muted">{t('upload.format')}</p>
      </div>
      <div className="space-y-4 p-4">
        <div className="space-y-1.5">
          <label htmlFor="courier" className="text-[12.5px] font-medium">{t('upload.courier')}</label>
          <select id="courier" required className="input" value={courierId} onChange={(e) => setCourierId(e.target.value)}>
            <option value="" disabled>—</option>
            {activeCouriers.map((c) => <option key={c.id} value={c.id}>{c.name} · {c.code}</option>)}
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <label htmlFor="periodStart" className="text-[12.5px] font-medium">{t('upload.periodStart')}</label>
            <input id="periodStart" type="date" className="input tabular-nums text-[13px]" value={periodStart} onChange={(e) => setPeriodStart(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <label htmlFor="periodEnd" className="text-[12.5px] font-medium">{t('upload.periodEnd')}</label>
            <input id="periodEnd" type="date" className="input tabular-nums text-[13px]" value={periodEnd} onChange={(e) => setPeriodEnd(e.target.value)} />
          </div>
        </div>

        <div
          role="button"
          tabIndex={0}
          data-testid="dropzone"
          data-dragging={dragging}
          onClick={() => inputRef.current?.click()}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), inputRef.current?.click())}
          onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          className={clsx(
            'focus-ring flex cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed px-4 py-7 text-center transition-colors',
            dragging ? 'border-accent bg-accent/[0.06]' : 'border-line bg-subtle/40 hover:border-faint hover:bg-subtle/70',
          )}
        >
          <input ref={inputRef} type="file" accept=".csv,text/csv" className="sr-only" data-testid="file-input"
            onChange={(e) => pick(e.target.files?.[0])} />
          {file ? (
            <div className="flex items-center gap-2.5 text-left">
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-signal/10 text-signal-ink-ink"><FileSpreadsheet className="h-4 w-4" /></span>
              <div className="min-w-0">
                <p className="truncate font-mono text-[13px] font-medium" data-testid="picked-file">{file.name}</p>
                <p className="text-[12px] text-faint">{humanSize(file.size)}</p>
              </div>
              <button type="button" aria-label="Remove file" className="focus-ring ml-2 rounded p-1 text-faint hover:bg-subtle hover:text-fg"
                onClick={(e) => { e.stopPropagation(); setFile(null); if (inputRef.current) inputRef.current.value = ''; }}>
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          ) : (
            <>
              <UploadCloud className={clsx('mb-2 h-6 w-6', dragging ? 'text-accent' : 'text-faint')} />
              <p className="text-[13px] font-medium">{dragging ? t('upload.dropActive') : t('upload.drop')}</p>
              <p className="mt-0.5 text-[12px] text-faint">{t('upload.browse')}</p>
            </>
          )}
        </div>

        {localError && <p role="alert" className="text-[12.5px] text-danger">{localError}</p>}
        {upload.isError && <ProblemAlert error={upload.error} />}

        {result?.duplicate && (
          <div role="status" data-testid="duplicate-notice" className="rounded-lg border border-warn/30 bg-warn/[0.07] px-3.5 py-3 text-[13px]">
            <p className="flex items-center gap-2 font-medium text-warn"><Copy className="h-4 w-4" />{t('upload.duplicateTitle')}</p>
            <p className="mt-1 text-warn/90">{t('upload.duplicate', { id: result.statement.id })}</p>
            <p className="mt-1 truncate font-mono text-[11px] text-warn/80" title={result.statement.fileSha256}>sha256 {result.statement.fileSha256}</p>
            <Link to={`/statements/${result.statement.id}`} className="mt-2 inline-block text-[12.5px] font-medium text-warn underline underline-offset-2" data-testid="open-existing">
              {t('upload.openExisting', { id: result.statement.id })}
            </Link>
          </div>
        )}
        {result && !result.duplicate && (
          <div role="status" data-testid="upload-accepted" className="flex items-center justify-between gap-2 rounded-lg border border-signal/30 bg-signal/[0.07] px-3.5 py-2.5 text-[13px] text-signal-ink-ink">
            <span className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4" />{t('upload.accepted', { id: result.statement.id })}</span>
            <Link to={`/statements/${result.statement.id}`} className="shrink-0 font-medium underline underline-offset-2" data-testid="open-uploaded">#{result.statement.id}</Link>
          </div>
        )}
      </div>
      <div className="mt-auto border-t border-line bg-subtle/50 px-4 py-3">
        <Button type="submit" variant="primary" className="w-full" loading={upload.isPending} disabled={!courierId} data-testid="upload-submit">
          {!upload.isPending && <UploadCloud className="h-4 w-4" />}{t('upload.submit')}
        </Button>
      </div>
    </form>
  );
}
