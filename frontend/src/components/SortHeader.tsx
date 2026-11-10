export type SortDir = 'asc' | 'desc';

/** Header kolom yang bisa diurutkan: tombol dengan aria-sort pada <th>; panah menunjukkan arah. */
export function SortHeader({ label, field, sort, onSort, numeric, className = '' }: {
  label: string;
  field: string;
  sort: { field: string; dir: SortDir };
  onSort(field: string): void;
  numeric?: boolean;
  className?: string;
}) {
  const active = sort.field === field;
  return (
    <th scope="col" className={`${numeric ? 'n' : ''} ${className}`} aria-sort={active ? (sort.dir === 'asc' ? 'ascending' : 'descending') : undefined}>
      <button type="button" onClick={() => onSort(field)} style={numeric ? { justifyContent: 'flex-end', width: '100%' } : undefined}>
        {label}
        <i className="arr" aria-hidden="true" />
      </button>
    </th>
  );
}

export function nextSort(current: { field: string; dir: SortDir }, field: string, defaultDir: SortDir = 'asc'): { field: string; dir: SortDir } {
  if (current.field !== field) return { field, dir: defaultDir };
  return { field, dir: current.dir === 'asc' ? 'desc' : 'asc' };
}
