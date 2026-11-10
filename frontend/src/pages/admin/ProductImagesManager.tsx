import { useRef, useState } from 'react';
import { useImageMutations } from '../../api/queries';
import type { Product, ProductImage } from '../../api/types';
import { uploadImage, UploadError } from '../../api/upload';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useToast } from '../../components/Toast';
import { messageOf } from '../../lib/errors';
import { ALLOWED_IMAGE_TYPES, MAX_IMAGES, moveImage, validateImageFile } from '../../lib/images';
import { srcSetOf } from '../../components/ProductVisual';

interface Pending {
  key: number;
  file: File;
  percent: number;
  error: string | null;
  controller: AbortController;
}

/**
 * Pengelola foto: unggah langsung ke penyimpanan dengan progres, urutan (tombol Kiri/Kanan, bisa lewat keyboard),
 * jadikan utama, dan hapus dengan konfirmasi. State: mengunggah, gagal (dengan alasan dan coba lagi), kosong, dan penuh.
 */
export function ProductImagesManager({ product }: { product: Product }) {
  const m = useImageMutations(product.id);
  const toast = useToast();
  const input = useRef<HTMLInputElement>(null);
  const [pending, setPending] = useState<Pending[]>([]);
  const [status, setStatus] = useState('');
  const [toDelete, setToDelete] = useState<ProductImage | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const seq = useRef(0);

  const images = product.images;
  const uploading = pending.filter((p) => !p.error).length;
  const count = images.length + uploading;
  const full = count >= MAX_IMAGES;

  const patch = (key: number, change: Partial<Pending>) => setPending((list) => list.map((p) => (p.key === key ? { ...p, ...change } : p)));

  async function run(p: Pending) {
    patch(p.key, { error: null, percent: 0 });
    try {
      const sig = await m.sign();
      const res = await uploadImage(p.file, sig, (percent) => patch(p.key, { percent }), p.controller.signal);
      await m.register.mutateAsync({ publicId: res.public_id, version: res.version, signature: res.signature, width: res.width || undefined, height: res.height || undefined, format: res.format, bytes: res.bytes });
      setPending((list) => list.filter((x) => x.key !== p.key));
      setStatus(`Foto ${p.file.name} berhasil diunggah.`);
    } catch (e) {
      if (p.controller.signal.aborted) {
        setPending((list) => list.filter((x) => x.key !== p.key));
        return;
      }
      patch(p.key, { error: e instanceof UploadError ? e.message : messageOf(e) });
    }
  }

  function add(files: FileList | null) {
    if (!files) return;
    let used = count;
    const rejected: string[] = [];
    for (const file of Array.from(files)) {
      const problem = validateImageFile(file, used);
      if (problem) {
        rejected.push(`${file.name}: ${problem}`);
        continue;
      }
      used++;
      const p: Pending = { key: ++seq.current, file, percent: 0, error: null, controller: new AbortController() };
      setPending((list) => [...list, p]);
      void run(p);
    }
    setActionError(rejected.length ? rejected.join(' ') : null);
    if (input.current) input.current.value = '';
  }

  const ids = images.map((i) => i.id);
  const move = (id: number, dir: -1 | 1) => {
    const next = moveImage(ids, id, dir);
    if (next) m.reorder.mutate(next, { onSuccess: () => setStatus(`Foto dipindah ke posisi ${next.indexOf(id) + 1} dari ${next.length}.`), onError: (e) => setActionError(messageOf(e)) });
  };

  return (
    <section className="panel-box" id="foto" aria-labelledby="foto-judul">
      <h2 id="foto-judul">
        Foto produk <span className="muted" style={{ fontWeight: 400 }}>{images.length} dari {MAX_IMAGES} foto</span>
      </h2>
      <p className="muted small-note">jpg, png, atau webp, maksimal 5 MB per foto. Urutan mengikuti kotak dari kiri ke kanan; foto utama tampil di katalog.</p>
      {actionError && <div className="alert err" role="alert">{actionError}</div>}
      <p className="sr-only" role="status" aria-live="polite">{status}</p>

      <ul className="imgs">
        {images.map((img, i) => (
          <li className="tile" key={img.id}>
            <div className="vis has-photo" data-tone="2">
              {img.primary && <span className="primary-tag">Utama</span>}
              <img src={img.cardUrl} srcSet={srcSetOf(img)} sizes="220px" alt={`${product.name}, foto ${i + 1}`} loading="lazy" />
            </div>
            <div className="acts">
              <div className="grp">
                <button type="button" disabled={i === 0 || m.reorder.isPending} onClick={() => move(img.id, -1)} aria-label={`Geser foto ${i + 1} ke kiri`}>Kiri</button>
                <button type="button" disabled={i === images.length - 1 || m.reorder.isPending} onClick={() => move(img.id, 1)} aria-label={`Geser foto ${i + 1} ke kanan`}>Kanan</button>
              </div>
              <div className="grp">
                <button type="button" disabled={img.primary || m.makePrimary.isPending} onClick={() => m.makePrimary.mutate(img.id, { onSuccess: () => setStatus(`Foto ${i + 1} dijadikan foto utama.`), onError: (e) => setActionError(messageOf(e)) })}>Jadikan utama</button>
                <button type="button" className="del" onClick={() => setToDelete(img)} aria-label={`Hapus foto ${i + 1}`}>Hapus</button>
              </div>
            </div>
          </li>
        ))}
        {pending.map((p) => (
          <li className="tile" key={`p${p.key}`}>
            {p.error ? (
              <div className="vis err-tile">
                <div className="msg" role="alert">
                  <b>{p.file.name} gagal diunggah</b>
                  <span>{p.error}</span>
                </div>
              </div>
            ) : (
              <div className="vis" data-tone="5" aria-hidden="false">
                <div className="dim" />
                <div className="prog" role="progressbar" aria-valuenow={p.percent} aria-valuemin={0} aria-valuemax={100} aria-label={`Mengunggah ${p.file.name}`}>
                  <span>Mengunggah {p.percent}%</span>
                  <div className="bar"><i style={{ width: `${p.percent}%` }} /></div>
                </div>
              </div>
            )}
            <div className="acts">
              <div className="grp">
                {p.error ? (
                  <>
                    <button type="button" onClick={() => void run({ ...p, controller: new AbortController() })}>Coba lagi</button>
                    <button type="button" className="del" onClick={() => setPending((l) => l.filter((x) => x.key !== p.key))}>Buang</button>
                  </>
                ) : (
                  <button type="button" onClick={() => p.controller.abort()}>Batalkan unggahan</button>
                )}
              </div>
            </div>
          </li>
        ))}
        {!full && (
          <li>
            <button type="button" className="addtile" onClick={() => input.current?.click()}>
              <b>{images.length + pending.length === 0 ? 'Tambah foto pertama' : 'Tambah foto'}</b>
              <small>Pilih dari perangkat</small>
            </button>
          </li>
        )}
      </ul>
      {full && <p className="muted small-note">Batas {MAX_IMAGES} foto tercapai. Hapus satu foto untuk menambah yang baru.</p>}
      <input ref={input} type="file" hidden multiple accept={ALLOWED_IMAGE_TYPES.join(',')} onChange={(e) => add(e.target.files)} aria-label="Pilih foto produk" />

      <ConfirmDialog
        open={toDelete !== null}
        title="Hapus foto ini?"
        confirmLabel="Hapus foto"
        danger
        busy={m.remove.isPending}
        onClose={() => setToDelete(null)}
        onConfirm={() => toDelete && m.remove.mutate(toDelete.id, { onSuccess: () => { toast.notify('Foto dihapus.'); setToDelete(null); }, onError: (e) => { setActionError(messageOf(e)); setToDelete(null); } })}
      >
        Foto dihapus dari produk dan dari penyimpanan. Tindakan ini tidak bisa dibatalkan.
      </ConfirmDialog>
    </section>
  );
}
