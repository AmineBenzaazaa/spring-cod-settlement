import type { ReactNode } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import { useI18n } from '@/lib/i18n';

/** Right-hand drawer (Radix Dialog: focus trap, Escape, aria). */
export function Sheet({ open, onClose, title, children, testId }: {
  open: boolean; onClose: () => void; title: ReactNode; children: ReactNode; testId?: string;
}) {
  const { t } = useI18n();
  return (
    <Dialog.Root open={open} onOpenChange={(o) => !o && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-black/20" />
        <Dialog.Content
          className="fixed inset-y-0 right-0 z-50 flex w-full max-w-[460px] flex-col border-l border-line bg-panel shadow-lift focus:outline-none"
          data-testid={testId}
          aria-describedby={undefined}
        >
          <div className="flex items-center justify-between gap-3 border-b border-line px-5 py-4">
            <Dialog.Title className="min-w-0 text-[15px] font-semibold tracking-tight">{title}</Dialog.Title>
            <Dialog.Close className="focus-ring rounded-md p-1 text-faint hover:bg-subtle hover:text-fg" aria-label={t('actions.close')}>
              <X className="h-4 w-4" />
            </Dialog.Close>
          </div>
          <div className="scrollbar-thin min-h-0 flex-1 overflow-y-auto">{children}</div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

/** Centered modal with the same chrome as the sibling dashboard's create dialog. */
export function Modal({ open, onOpenChange, title, description, children, testId }: {
  open: boolean; onOpenChange: (o: boolean) => void; title: ReactNode; description?: ReactNode; children: ReactNode; testId?: string;
}) {
  const { t } = useI18n();
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-black/30 backdrop-blur-[2px]" />
        <Dialog.Content
          className="fixed left-1/2 top-[10vh] z-50 w-[calc(100vw-32px)] max-w-md -translate-x-1/2 rounded-2xl border border-line bg-panel shadow-lift focus:outline-none"
          data-testid={testId}
          aria-describedby={description ? 'modal-desc' : undefined}
        >
          <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
            <div>
              <Dialog.Title className="text-[15px] font-semibold">{title}</Dialog.Title>
              {description && <Dialog.Description id="modal-desc" className="mt-0.5 text-[12.5px] text-muted">{description}</Dialog.Description>}
            </div>
            <Dialog.Close className="focus-ring rounded-md p-1 text-faint hover:bg-subtle hover:text-fg" aria-label={t('actions.close')}>
              <X className="h-4 w-4" />
            </Dialog.Close>
          </div>
          {children}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
