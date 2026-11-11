import { useRef, type ImgHTMLAttributes, type PointerEvent } from 'react';

export const MAX_ZOOM = 4;
export const TAP_ZOOM = 2.5;
const TAP_MS = 300; // jeda maksimum antara dua ketukan
const TAP_SLOP = 10; // geser lebih dari ini bukan ketukan

type Pt = { x: number; y: number };

/** Geser maksimum satu sumbu: tepi gambar yang diperbesar tidak boleh menjauh dari tepi layar. */
export function clampPan(t: number, content: number, scale: number, view: number) {
  const max = Math.max(0, (content * scale - view) / 2);
  return Math.max(-max, Math.min(max, t));
}

/**
 * Gambar yang bisa diperbesar dengan cubit (dua jari), digeser saat diperbesar, dan ketuk dua kali untuk
 * zoom/kembali. Pointer Events + `touch-action: none` saja, tanpa pustaka. Posisi dihitung terhadap pusat
 * dialog; transformasi ditulis langsung ke elemen (tanpa render ulang tiap gerakan). Berganti `key` = reset.
 */
export function ZoomImage(props: ImgHTMLAttributes<HTMLImageElement>) {
  const img = useRef<HTMLImageElement>(null);
  const view = useRef({ s: 1, x: 0, y: 0 });
  const ptrs = useRef(new Map<number, Pt>());
  const pinch = useRef({ d: 1, s: 1, x: 0, y: 0, mx: 0, my: 0 });
  const pan = useRef({ px: 0, py: 0, x: 0, y: 0 });
  const tap = useRef({ t: 0, x: 0, y: 0, moved: false, last: -1e9, lx: 0, ly: 0 });

  const box = () => img.current?.closest('dialog') ?? document.documentElement;
  const rel = (e: PointerEvent): Pt => {
    const r = box().getBoundingClientRect();
    return { x: e.clientX - (r.left + r.width / 2), y: e.clientY - (r.top + r.height / 2) };
  };
  const apply = (s: number, x: number, y: number) => {
    const el = img.current;
    if (!el) return;
    s = Math.max(1, Math.min(MAX_ZOOM, s));
    const b = box();
    x = s === 1 ? 0 : clampPan(x, el.offsetWidth, s, b.clientWidth);
    y = s === 1 ? 0 : clampPan(y, el.offsetHeight, s, b.clientHeight);
    view.current = { s, x, y };
    el.style.transform = s === 1 ? '' : `translate(${x}px, ${y}px) scale(${s})`;
    el.dataset.zoom = s === 1 ? '1' : s.toFixed(2);
  };
  /** Ubah skala ke s2 dengan titik p (relatif pusat) tetap di bawah jari. */
  const zoomAt = (s2: number, p: Pt) => {
    const { s, x, y } = view.current;
    const q = { x: (p.x - x) / s, y: (p.y - y) / s };
    apply(s2, p.x - s2 * q.x, p.y - s2 * q.y);
  };
  const beginPan = (p: Pt) => (pan.current = { px: p.x, py: p.y, x: view.current.x, y: view.current.y });
  const pair = () => {
    const [a, b] = [...ptrs.current.values()] as [Pt, Pt];
    return { d: Math.hypot(a.x - b.x, a.y - b.y) || 1, mx: (a.x + b.x) / 2, my: (a.y + b.y) / 2 };
  };

  const down = (e: PointerEvent) => {
    const p = rel(e);
    ptrs.current.set(e.pointerId, p);
    img.current?.setPointerCapture?.(e.pointerId);
    if (ptrs.current.size === 2) {
      pinch.current = { ...pair(), ...view.current };
      tap.current.moved = true;
    } else {
      tap.current = { ...tap.current, t: e.timeStamp, x: p.x, y: p.y, moved: false };
      beginPan(p);
    }
  };
  const move = (e: PointerEvent) => {
    if (!ptrs.current.has(e.pointerId)) return;
    const p = rel(e);
    ptrs.current.set(e.pointerId, p);
    if (ptrs.current.size === 2) {
      const { d, mx, my } = pair();
      const g = pinch.current;
      const s = Math.max(1, Math.min(MAX_ZOOM, (g.s * d) / g.d));
      const qx = (g.mx - g.x) / g.s, qy = (g.my - g.y) / g.s; // titik gambar di bawah pusat cubitan awal
      apply(s, mx - s * qx, my - s * qy);
    } else {
      if (Math.hypot(p.x - tap.current.x, p.y - tap.current.y) > TAP_SLOP) tap.current.moved = true;
      if (view.current.s > 1) apply(view.current.s, pan.current.x + p.x - pan.current.px, pan.current.y + p.y - pan.current.py);
    }
  };
  const up = (e: PointerEvent, cancelled = false) => {
    const p = ptrs.current.get(e.pointerId);
    if (!p) return;
    const wasSingle = ptrs.current.size === 1;
    ptrs.current.delete(e.pointerId);
    if (!wasSingle) {
      const [rest] = [...ptrs.current.values()]; // satu jari tersisa: lanjut menggeser dari posisinya
      if (rest) beginPan(rest);
      return;
    }
    const t = tap.current;
    if (cancelled || t.moved || e.timeStamp - t.t > TAP_MS) return;
    if (e.timeStamp - t.last < TAP_MS && Math.hypot(p.x - t.lx, p.y - t.ly) < 30) {
      t.last = -1e9;
      if (view.current.s > 1) apply(1, 0, 0);
      else zoomAt(TAP_ZOOM, p);
    } else {
      t.last = e.timeStamp; t.lx = p.x; t.ly = p.y;
    }
  };

  return <img {...props} ref={img} data-zoom="1" draggable={false} onPointerDown={down} onPointerMove={move} onPointerUp={up} onPointerCancel={(e) => up(e, true)} />;
}
