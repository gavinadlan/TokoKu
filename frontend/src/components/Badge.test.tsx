import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge } from './Badge';
import { StatusLabel } from './StatusLabel';

describe('Badge', () => {
  it('memakai kelas nada dan menampilkan teks utuh', () => {
    render(<Badge tone="pending">Menunggu pembayaran</Badge>);
    const el = screen.getByText('Menunggu pembayaran');
    expect(el).toHaveClass('badge', 'badge-pending');
  });
  it('nada bawaan netral', () => {
    render(<Badge>Dikirim</Badge>);
    expect(screen.getByText('Dikirim')).toHaveClass('badge-neutral');
  });
  it('StatusLabel memakai Badge dengan nada status', () => {
    render(<><StatusLabel status="COMPLETED" /><StatusLabel status="CANCELLED" /></>);
    expect(screen.getByText('Selesai')).toHaveClass('badge', 'badge-done');
    expect(screen.getByText('Dibatalkan')).toHaveClass('badge-cancelled');
  });
});
