import { useState } from 'react';
import { useCategories, useDeleteCategory, useSaveCategory } from '../../api/queries';
import type { Category } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Field } from '../../components/Field';
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States';
import { messageOf } from '../../lib/errors';
import { useDocumentTitle } from '../../lib/useDocumentTitle';

function Row({ category, onDelete }: { category: Category; onDelete(): void }) {
  const save = useSaveCategory();
  const [name, setName] = useState(category.name);
  const [editing, setEditing] = useState(false);

  if (!editing) {
    return (
      <tr>
        <td>{category.name}</td>
        <td style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>
          <button type="button" className="link-btn" onClick={() => setEditing(true)} aria-label={`Ubah nama ${category.name}`}>Ubah nama</button>
          <button type="button" className="link-btn" style={{ marginLeft: 'var(--s-4)' }} onClick={onDelete} aria-label={`Hapus ${category.name}`}>Hapus</button>
        </td>
      </tr>
    );
  }
  return (
    <tr>
      <td colSpan={2}>
        <form
          className="inline-form"
          onSubmit={async (e) => {
            e.preventDefault();
            try {
              await save.mutateAsync({ id: category.id, name: name.trim() });
              setEditing(false);
            } catch {
              /* pesan ditampilkan di bawah */
            }
          }}
        >
          <Field label="Nama kategori" error={save.error ? messageOf(save.error) : undefined}>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} maxLength={100} required />
          </Field>
          <button className="btn small" type="submit" disabled={save.isPending || name.trim() === ''}>Simpan</button>
          <button className="btn ghost small" type="button" onClick={() => { setEditing(false); setName(category.name); save.reset(); }}>Batal</button>
        </form>
      </td>
    </tr>
  );
}

export function AdminCategoriesPage() {
  useDocumentTitle('Kelola kategori');
  const categories = useCategories();
  const create = useSaveCategory();
  const del = useDeleteCategory();
  const [name, setName] = useState('');
  const [toDelete, setToDelete] = useState<Category | null>(null);

  return (
    <>
      <h1>Kategori</h1>
      <form
        className="inline-form"
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            await create.mutateAsync({ name: name.trim() });
            setName('');
          } catch {
            /* ditampilkan di bawah */
          }
        }}
      >
        <Field label="Kategori baru" error={create.error ? messageOf(create.error) : undefined}>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} maxLength={100} />
        </Field>
        <button className="btn" type="submit" disabled={create.isPending || name.trim() === ''}>Tambah kategori</button>
      </form>

      {del.error && <div className="alert err" role="alert">{messageOf(del.error)}</div>}

      {categories.isPending ? (
        <ListSkeleton rows={3} />
      ) : categories.isError ? (
        <ErrorState error={categories.error} onRetry={() => categories.refetch()} />
      ) : categories.data.length === 0 ? (
        <EmptyState title="Belum ada kategori">Tambahkan kategori pertama di atas.</EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="data" style={{ minWidth: 0 }}>
            <thead><tr><th>Nama</th><th><span className="sr-only">Aksi</span></th></tr></thead>
            <tbody>
              {categories.data.map((c) => (
                <Row key={c.id} category={c} onDelete={() => { del.reset(); setToDelete(c); }} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={toDelete !== null}
        title="Hapus kategori ini?"
        confirmLabel="Hapus"
        danger
        busy={del.isPending}
        onClose={() => setToDelete(null)}
        onConfirm={() => toDelete && del.mutate(toDelete.id, { onSettled: () => setToDelete(null) })}
      >
        {toDelete?.name} akan dihapus. Kategori yang masih punya produk tidak bisa dihapus.
      </ConfirmDialog>
    </>
  );
}
