import type { ReactNode } from 'react';

export type BadgeTone = 'neutral' | 'accent' | 'pending' | 'active' | 'done' | 'cancelled';

/** Satu-satunya pil status/jenis di aplikasi: satu baris, tinggi mengikuti isi, teks tidak pernah terpotong. */
export function Badge({ tone = 'neutral', children }: { tone?: BadgeTone; children: ReactNode }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}
