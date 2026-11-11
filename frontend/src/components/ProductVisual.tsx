import type { ProductImage } from '../api/types';
import { productNumber } from '../lib/format';
import { toneFor } from '../lib/tone';

interface Props {
  id: number;
  categoryId: number;
  name: string;
  /** Foto produk (urut). Foto utama dipakai; tanpa foto: label kemasan yang dirancang. */
  images?: ProductImage[];
  /** URL tunggal (mis. thumbnail dari keranjang). */
  imageUrl?: string | null;
  size?: 'card' | 'big' | 'thumb';
}

/** srcset dari empat ukuran bernama (160, 480, 960, 1600 px lebar). */
export function srcSetOf(img: ProductImage): string {
  return `${img.thumbnailUrl} 160w, ${img.cardUrl} 480w, ${img.detailUrl} 960w, ${img.largeUrl} 1600w`;
}

const SIZES: Record<NonNullable<Props['size']>, string> = {
  card: '(min-width: 1100px) 280px, (min-width: 768px) 30vw, 46vw',
  big: '(min-width: 1024px) 560px, 100vw',
  thumb: '64px',
};

/**
 * Visual produk. Dengan foto: gambar di dalam bingkai tipis (bingkai label kemasan tetap menjadi motif).
 * Tanpa foto: placeholder label kemasan dengan nomor produk besar (dari id database), bukan kotak abu-abu kosong.
 */
export function ProductVisual({ id, categoryId, name, images, imageUrl, size = 'card' }: Props) {
  const cls = size === 'card' ? 'vis' : `vis ${size}`;
  const tone = toneFor(categoryId, id);
  const main = images?.find((i) => i.primary) ?? images?.[0];
  if (main) {
    return (
      <div className={`${cls} has-photo`} data-tone={tone}>
        <img src={size === 'thumb' ? main.thumbnailUrl : size === 'big' ? main.detailUrl : main.cardUrl} srcSet={srcSetOf(main)} sizes={SIZES[size]} alt={name} loading={size === 'big' ? 'eager' : 'lazy'} width={main.width ?? undefined} height={main.height ?? undefined} />
      </div>
    );
  }
  if (imageUrl) {
    return (
      <div className={`${cls} has-photo`} data-tone={tone}>
        <img src={imageUrl} alt={name} loading="lazy" />
      </div>
    );
  }
  return (
    <div className={cls} data-tone={tone} aria-hidden="true">
      <span className="num">{String(id).padStart(2, '0')}</span>
      <span className="cap">No. {productNumber(id)}</span>
    </div>
  );
}
