import { useEffect, useRef, type ReactNode } from 'react';

interface Props {
  open: boolean;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm(): void;
  onClose(): void;
}

/** Dialog konfirmasi memakai elemen <dialog> bawaan: fokus terkurung dan Escape menutup tanpa pustaka tambahan. */
export function ConfirmDialog({ open, title, children, confirmLabel, cancelLabel = 'Batal', danger, busy, onConfirm, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (open && !el.open) el.showModal();
    if (!open && el.open) el.close();
  }, [open]);

  return (
    <dialog ref={ref} className="dlg" aria-labelledby="dlg-title" onClose={onClose}>
      <h2 id="dlg-title">{title}</h2>
      <div className="muted">{children}</div>
      <div className="actions">
        <button type="button" className="btn ghost small" onClick={onClose}>
          {cancelLabel}
        </button>
        <button type="button" className={`btn small ${danger ? 'danger' : ''}`} onClick={onConfirm} disabled={busy}>
          {busy ? 'Memproses...' : confirmLabel}
        </button>
      </div>
    </dialog>
  );
}
