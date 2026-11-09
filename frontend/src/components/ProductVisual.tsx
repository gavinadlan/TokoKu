import { productNumber } from '../lib/format';
import { toneFor } from '../lib/tone';

interface Props {
  id: number;
  categoryId: number;
  name: string;
  imageUrl?: string | null;
  size?: 'card' | 'big' | 'thumb';
}

/**
 * Visual produk. Dengan imageUrl: foto di dalam bingkai tipis. Tanpa foto: label kemasan yang dirancang
 * (nomor produk besar dari id database), bukan kotak abu-abu kosong.
 */
export function ProductVisual({ id, categoryId, name, imageUrl, size = 'card' }: Props) {
  const cls = size === 'card' ? 'vis' : `vis ${size}`;
  if (imageUrl) {
    return (
      <div className={cls} data-tone={toneFor(categoryId, id)}>
        <img src={imageUrl} alt={name} loading="lazy" />
      </div>
    );
  }
  return (
    <div className={cls} data-tone={toneFor(categoryId, id)} aria-hidden="true">
      <span className="num">{String(id).padStart(2, '0')}</span>
      <span className="cap">No. {productNumber(id)}</span>
    </div>
  );
}
