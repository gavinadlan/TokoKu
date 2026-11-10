import { useState, type KeyboardEvent } from 'react';
import type { ProductImage } from '../api/types';
import { srcSetOf } from './ProductVisual';

/**
 * Galeri produk: panggung besar + thumbnail, tombol sebelumnya/berikutnya dengan penghitung (aria-live),
 * dan navigasi panah kiri/kanan di keyboard. Gambar memakai srcset tiga ukuran.
 */
export function Gallery({ images, name }: { images: ProductImage[]; name: string }) {
  const start = Math.max(0, images.findIndex((i) => i.primary));
  const [index, setIndex] = useState(start);
  const current = images[Math.min(index, images.length - 1)]!;
  const go = (delta: number) => setIndex((i) => (i + delta + images.length) % images.length);

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'ArrowLeft') {
      e.preventDefault();
      go(-1);
    } else if (e.key === 'ArrowRight') {
      e.preventDefault();
      go(1);
    }
  };

  return (
    <div className="gallery" role="group" aria-roledescription="galeri" aria-label={`Foto ${name}`} onKeyDown={onKey}>
      <div className="stage">
        <div className="vis big has-photo" data-tone="1">
          <img src={current.detailUrl} srcSet={srcSetOf(current)} sizes="(min-width: 1024px) 560px, 100vw" alt={`${name}, foto ${index + 1} dari ${images.length}`} width={current.width ?? undefined} height={current.height ?? undefined} />
        </div>
        {images.length > 1 && (
          <div className="nav2">
            <button type="button" aria-label="Foto sebelumnya" onClick={() => go(-1)}>
              <span aria-hidden="true">&lsaquo;</span>
            </button>
            <output aria-live="polite">
              {index + 1} / {images.length}
            </output>
            <button type="button" aria-label="Foto berikutnya" onClick={() => go(1)}>
              <span aria-hidden="true">&rsaquo;</span>
            </button>
          </div>
        )}
      </div>
      {images.length > 1 && (
        <div className="thumbs" role="group" aria-label="Pilih foto">
          {images.map((img, i) => (
            <button key={img.id} type="button" aria-label={`Foto ${i + 1}`} aria-current={i === index} onClick={() => setIndex(i)}>
              <div className="vis has-photo" data-tone="1">
                <img src={img.thumbnailUrl} alt="" loading="lazy" />
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
