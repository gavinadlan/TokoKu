import { useEffect, useRef, type FormEvent, type ReactNode } from 'react';

/** Dialog berformulir (elemen <dialog> bawaan): fokus terkurung, Escape menutup, fokus kembali ke pemicu. */
export function FormDialog({ open, title, children, submitLabel, busy, error, onSubmit, onClose }: {
  open: boolean;
  title: string;
  children: ReactNode;
  submitLabel: string;
  busy?: boolean;
  error?: string | null;
  onSubmit(): void;
  onClose(): void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (open && !el.open) el.showModal();
    if (!open && el.open) el.close();
  }, [open]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <dialog ref={ref} className="dlg" aria-labelledby="fd-title" onClose={onClose}>
      <form onSubmit={submit} className="form" style={{ gap: 'var(--s-4)' }}>
        <h2 id="fd-title">{title}</h2>
        {error && (
          <div className="alert err" role="alert">
            {error}
          </div>
        )}
        {children}
        <div className="actions">
          <button type="button" className="btn ghost small" onClick={onClose}>
            Batal
          </button>
          <button type="submit" className="btn small" disabled={busy}>
            {busy ? 'Memproses...' : submitLabel}
          </button>
        </div>
      </form>
    </dialog>
  );
}
