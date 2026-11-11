import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

interface ToastItem {
  id: number;
  message: string;
  action?: ReactNode;
}

const ToastContext = createContext<{ notify(message: string, action?: ReactNode): void } | null>(null);

/** Umpan balik singkat; wilayah aria-live memastikan pembaca layar ikut mengumumkannya. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastItem | null>(null);
  const [paused, setPaused] = useState(false);
  const notify = useCallback((message: string, action?: ReactNode) => setToast({ id: Date.now(), message, action }), []);

  // Toast hilang sendiri setelah 5 detik, tetapi dijeda saat disorot atau difokus (WCAG 2.2.1)
  useEffect(() => {
    if (!toast || paused) return;
    const t = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(t);
  }, [toast, paused]);

  const value = useMemo(() => ({ notify }), [notify]);
  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-region" role="status" aria-live="polite">
        {toast && (
          <div className="toast" key={toast.id} onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)} onFocus={() => setPaused(true)} onBlur={() => setPaused(false)}>
            <span>{toast.message}</span>
            {toast.action}
            <button type="button" className="toast-x" aria-label="Tutup pesan" onClick={() => setToast(null)}>
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
        )}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast harus dipakai di dalam ToastProvider');
  return ctx;
}
