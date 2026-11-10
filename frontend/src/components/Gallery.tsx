import { useEffect, useRef, useState, type KeyboardEvent, type MouseEvent, type ReactNode } from 'react';
import type { ProductImage } from '../api/types';
import { srcSetOf } from './ProductVisual';

// Lebar tampil panggung: kolom galeri ±58% dari konten 1200px di desktop, selebar layar di ponsel.
const STAGE_SIZES = '(min-width: 1024px) min(56vw, 670px), 100vw';
// Lightbox: gambar 4:5 dibatasi tinggi layar, jadi lebarnya 80vh kecuali di layar tegak.
const ZOOM_SIZES = '(min-aspect-ratio: 4/5) 80vh, 100vw';

interface Props {
  images: ProductImage[];
  name: string;
  /** Ditampilkan bila produk belum punya foto (label kemasan). */
  placeholder?: ReactNode;
}

/**
 * Galeri detail produk. Slider memakai CSS scroll-snap (geser dengan sentuhan tanpa pustaka), tombol
 * sebelumnya/berikutnya, thumbnail, panah kiri/kanan di keyboard, dan lightbox dari <dialog> bawaan
 * (fokus terkurung, Escape menutup, fokus kembali ke pemicu). Satu foto: tanpa kontrol. Tanpa foto: placeholder.
 */
export function Gallery({ images, name, placeholder }: Props) {
  const photos = [...images].sort((a, b) => Number(b.primary) - Number(a.primary)); // foto utama dulu, sisanya sesuai urutan admin
  const count = photos.length;
  const [index, setIndex] = useState(0);
  const [zoomIndex, setZoomIndex] = useState<number | null>(null);
  const track = useRef<HTMLDivElement>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const at = Math.min(index, count - 1);
  const zoomed = zoomIndex === null ? null : photos[Math.min(zoomIndex, count - 1)];

  useEffect(() => {
    const el = dialog.current;
    if (zoomIndex !== null && el && !el.open) el.showModal();
  }, [zoomIndex]);

  if (count === 0) return <div className="gallery">{placeholder}</div>;

  const clamp = (i: number) => Math.max(0, Math.min(count - 1, i));
  const go = (i: number) => {
    const el = track.current;
    setIndex(clamp(i));
    // ponytail: pindah langsung tanpa animasi (geser sentuh tetap halus); gulir halus butuh penanganan event scroll antara
    el?.scrollTo?.({ left: clamp(i) * el.clientWidth, behavior: 'instant' });
  };
  const step = (delta: number) => (zoomIndex === null ? go(at + delta) : setZoomIndex(clamp(zoomIndex + delta)));

  const onScroll = () => {
    const el = track.current;
    if (el && el.clientWidth > 0) setIndex(clamp(Math.round(el.scrollLeft / el.clientWidth)));
  };
  const onKey = (e: KeyboardEvent) => {
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
    e.preventDefault();
    step(e.key === 'ArrowLeft' ? -1 : 1);
  };
  const onBackdrop = (e: MouseEvent) => {
    if (!(e.target as Element).closest('img, button')) dialog.current?.close();
  };

  const alt = (i: number) => (count > 1 ? `${name}, foto ${i + 1} dari ${count}` : name);
  const shown = zoomIndex ?? at;
  const arrows = (cls: string) => (
    <>
      <button type="button" className={`${cls} prev`} aria-label="Foto sebelumnya" aria-disabled={shown === 0} onClick={() => step(-1)}>
        <span aria-hidden="true">&lsaquo;</span>
      </button>
      <button type="button" className={`${cls} next`} aria-label="Foto berikutnya" aria-disabled={shown === count - 1} onClick={() => step(1)}>
        <span aria-hidden="true">&rsaquo;</span>
      </button>
    </>
  );

  return (
    <div className="gallery" data-multi={count > 1 || undefined} role="group" aria-roledescription="galeri" aria-label={`Foto ${name}`} onKeyDown={onKey}>
      <div className="stage">
        <div className="track" ref={track} onScroll={onScroll}>
          {photos.map((img, i) => (
            <div key={img.id} className="slide" role={count > 1 ? 'group' : undefined} aria-roledescription={count > 1 ? 'slide' : undefined} aria-label={count > 1 ? `${i + 1} dari ${count}` : undefined}>
              <button type="button" className="zoom" aria-haspopup="dialog" tabIndex={i === at ? 0 : -1} onClick={() => setZoomIndex(i)}>
                <span className="vis big has-photo">
                  <img src={img.detailUrl} srcSet={srcSetOf(img)} sizes={STAGE_SIZES} alt={alt(i)} width={960} height={1200} loading={i === 0 ? 'eager' : 'lazy'} fetchPriority={i === 0 ? 'high' : undefined} decoding="async" />
                </span>
                <span className="sr-only">, perbesar</span>
              </button>
            </div>
          ))}
        </div>
        {count > 1 && zoomIndex === null && arrows('nav')}
        {count > 1 && (
          <output className="count" aria-live="polite">
            {at + 1} / {count}
          </output>
        )}
      </div>
      {count > 1 && (
        <div className="thumbs" role="group" aria-label="Pilih foto">
          {photos.map((img, i) => (
            <button key={img.id} type="button" aria-label={`Foto ${i + 1}`} aria-current={i === at ? 'true' : undefined} onClick={() => go(i)}>
              <img src={img.thumbnailUrl} alt="" width={160} height={200} loading="lazy" />
            </button>
          ))}
        </div>
      )}
      <dialog ref={dialog} className="lightbox" aria-label={`Foto ${name}, tampilan besar`} onClose={() => setZoomIndex(null)} onClick={onBackdrop}>
        {zoomed && zoomIndex !== null && (
          <>
            <button type="button" className="lb-btn close" onClick={() => dialog.current?.close()}>
              Tutup
            </button>
            <img key={zoomed.id} src={zoomed.largeUrl} srcSet={`${zoomed.cardUrl} 480w, ${zoomed.detailUrl} 960w, ${zoomed.largeUrl} 1600w`} sizes={ZOOM_SIZES} alt={alt(zoomIndex)} />
            {count > 1 && (
              <>
                {arrows('lb-btn')}
                <span className="lb-count" aria-live="polite">
                  {zoomIndex + 1} / {count}
                </span>
              </>
            )}
          </>
        )}
      </dialog>
    </div>
  );
}
