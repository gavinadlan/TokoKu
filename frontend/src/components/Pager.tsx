interface Props {
  page: number; // 1-based
  totalPages: number;
  onChange(page: number): void;
}

export function Pager({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pager" aria-label="Halaman">
      <button className="btn ghost small" type="button" disabled={page <= 1} onClick={() => onChange(page - 1)}>
        Sebelumnya
      </button>
      <span className="tnum muted" aria-live="polite">
        Halaman {page} dari {totalPages}
      </span>
      <button className="btn ghost small" type="button" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        Berikutnya
      </button>
    </nav>
  );
}
