import { fireEvent, render, screen, within } from '@testing-library/react';
import { beforeAll, describe, expect, it } from 'vitest';
import type { ProductImage } from '../api/types';
import { Gallery } from './Gallery';

const photo = (id: number, primary = false): ProductImage => ({
  id, primary, position: id, width: 1200, height: 1500,
  thumbnailUrl: `/t${id}.jpg`, cardUrl: `/c${id}.jpg`, detailUrl: `/d${id}.jpg`, largeUrl: `/l${id}.jpg`,
});
const five = [1, 2, 3, 4, 5].map((id) => photo(id, id === 1));

beforeAll(() => {
  // jsdom belum tentu punya showModal/close; perilaku modal sungguhan (fokus, Escape) diuji di e2e
  HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) { this.setAttribute('open', ''); };
  HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) { this.removeAttribute('open'); this.dispatchEvent(new Event('close')); };
});

const next = () => screen.getByRole('button', { name: 'Foto berikutnya' });
const prev = () => screen.getByRole('button', { name: 'Foto sebelumnya' });
const counter = () => screen.getByRole('status');

describe('Gallery', () => {
  it('tanpa foto: placeholder, tanpa kontrol', () => {
    render(<Gallery images={[]} name="Kopi Gayo" placeholder={<div data-testid="label">No. 001</div>} />);
    expect(screen.getByTestId('label')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.queryByRole('img')).toBeNull();
  });

  it('satu foto: tanpa panah, thumbnail, dan penanda; dimuat prioritas; masih bisa diperbesar', () => {
    render(<Gallery images={[photo(1, true)]} name="Kopi Gayo" />);
    const img = screen.getByRole('img', { name: 'Kopi Gayo' });
    expect(img).toHaveAttribute('loading', 'eager');
    expect(img).toHaveAttribute('fetchpriority', 'high');
    expect(img.getAttribute('srcset')).toMatch(/160w.*480w.*960w.*1600w/);
    expect(screen.queryByRole('button', { name: 'Foto berikutnya' })).toBeNull();
    expect(screen.queryByRole('group', { name: 'Pilih foto' })).toBeNull();
    expect(screen.queryByRole('status')).toBeNull();
    expect(screen.getAllByRole('button')).toHaveLength(1);
  });

  it('banyak foto: foto utama dulu, hanya foto pertama dimuat prioritas', () => {
    render(<Gallery images={[photo(1), photo(2), photo(3, true)]} name="Kopi Gayo" />);
    const imgs = screen.getAllByRole('img', { name: /Kopi Gayo, foto/ });
    expect(imgs.map((i) => i.getAttribute('src'))).toEqual(['/d3.jpg', '/d1.jpg', '/d2.jpg']);
    expect(imgs.map((i) => i.getAttribute('loading'))).toEqual(['eager', 'lazy', 'lazy']);
    expect(imgs[1]).toHaveAccessibleName('Kopi Gayo, foto 2 dari 3');
  });

  it('tombol berikutnya/sebelumnya memindah foto dan nonaktif di ujung', () => {
    render(<Gallery images={five} name="Kopi Gayo" />);
    expect(counter()).toHaveTextContent('1 / 5');
    expect(prev()).toHaveAttribute('aria-disabled', 'true');
    fireEvent.click(prev());
    expect(counter()).toHaveTextContent('1 / 5');
    fireEvent.click(next());
    expect(counter()).toHaveTextContent('2 / 5');
    expect(prev()).toHaveAttribute('aria-disabled', 'false');
    for (let i = 0; i < 6; i++) fireEvent.click(next());
    expect(counter()).toHaveTextContent('5 / 5');
    expect(next()).toHaveAttribute('aria-disabled', 'true');
  });

  it('thumbnail menandai foto aktif dan bisa diklik', () => {
    render(<Gallery images={five} name="Kopi Gayo" />);
    const thumbs = within(screen.getByRole('group', { name: 'Pilih foto' })).getAllByRole('button');
    expect(thumbs).toHaveLength(5);
    expect(thumbs[0]).toHaveAttribute('aria-current', 'true');
    fireEvent.click(thumbs[3]!);
    expect(thumbs[3]).toHaveAttribute('aria-current', 'true');
    expect(thumbs[0]).not.toHaveAttribute('aria-current');
    expect(counter()).toHaveTextContent('4 / 5');
  });

  it('panah kiri/kanan di keyboard saat fokus ada di galeri', () => {
    render(<Gallery images={five} name="Kopi Gayo" />);
    const gallery = screen.getByRole('group', { name: 'Foto Kopi Gayo' });
    fireEvent.keyDown(gallery, { key: 'ArrowRight' });
    fireEvent.keyDown(gallery, { key: 'ArrowRight' });
    expect(counter()).toHaveTextContent('3 / 5');
    fireEvent.keyDown(gallery, { key: 'ArrowLeft' });
    expect(counter()).toHaveTextContent('2 / 5');
    fireEvent.keyDown(gallery, { key: 'Enter' });
    expect(counter()).toHaveTextContent('2 / 5');
  });

  it('klik foto membuka lightbox dengan ukuran besar, navigasi, dan tombol tutup', () => {
    render(<Gallery images={five} name="Kopi Gayo" />);
    fireEvent.click(screen.getByRole('button', { name: /foto 1 dari 5.*perbesar/ }));
    const dialog = screen.getByRole('dialog', { name: /tampilan besar/ });
    expect(dialog).toHaveAttribute('open');
    const big = () => within(dialog).getByRole('img');
    expect(big()).toHaveAttribute('src', '/l1.jpg');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Foto berikutnya' }));
    expect(big()).toHaveAttribute('src', '/l2.jpg');
    fireEvent.keyDown(dialog, { key: 'ArrowRight' });
    expect(big()).toHaveAccessibleName('Kopi Gayo, foto 3 dari 5');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Tutup' }));
    expect(within(dialog).queryByRole('img')).toBeNull();
    expect(counter()).toHaveTextContent('1 / 5'); // galeri tetap di foto pemicu, tempat fokus kembali
  });
});
