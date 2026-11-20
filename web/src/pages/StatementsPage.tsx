import { useState } from 'react';
import { Info } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { UploadCard } from '@/components/UploadCard';
import { StatementsTable } from '@/components/StatementsTable';
import { PageHeader } from '@/components/ui/PageHeader';

export function StatementsPage() {
  const { t } = useI18n();
  const { isStaff } = useAuth();
  const [page, setPage] = useState(0);
  const [highlight, setHighlight] = useState<number | null>(null);

  return (
    <>
      <PageHeader title={t('statements.title')} subtitle={t('statements.subtitle')} />
      <div className="grid items-start gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
        {isStaff ? (
          <div className="panel flex gap-3 p-4 text-[13px] text-muted" data-testid="staff-upload-note">
            <Info className="mt-0.5 h-4 w-4 shrink-0 text-accent" />
            <p>{t('upload.staffNote')}</p>
          </div>
        ) : (
          <UploadCard onUploaded={(res) => { setPage(0); setHighlight(res.statement.id); }} />
        )}
        <StatementsTable page={page} onPage={setPage} highlight={highlight} />
      </div>
    </>
  );
}
