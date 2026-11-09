import { Link, useNavigate } from 'react-router';
import { useCart, useRemoveCartItem, useUpdateCartItem } from '../api/queries';
import { ProductVisual } from '../components/ProductVisual';
import { Receipt } from '../components/Receipt';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { clampQuantity } from '../lib/cart';
import { messageOf } from '../lib/errors';
import { formatRupiah } from '../lib/format';
import { useDocumentTitle } from '../lib/useDocumentTitle';

export function CartPage() {
  useDocumentTitle('Keranjang');
  const cart = useCart();
  const update = useUpdateCartItem();
  const remove = useRemoveCartItem();
  const navigate = useNavigate();

  const mutationError = update.error ?? remove.error;

  return (
    <div className="wrap page-pad">
      <h1 className="page-title" style={{ marginBottom: 'var(--s-6)' }}>
        Keranjang
      </h1>
      {cart.isPending ? (
        <ListSkeleton />
      ) : cart.isError ? (
        <ErrorState error={cart.error} onRetry={() => cart.refetch()} title="Keranjang belum bisa dimuat" />
      ) : cart.data.items.length === 0 ? (
        <EmptyState
          title="Keranjang Anda masih kosong"
          actions={
            <Link className="btn" to="/">
              Lihat katalog
            </Link>
          }
        >
          Pilih biji kopi atau alat seduh, lalu tambahkan ke sini.
        </EmptyState>
      ) : (
        <div className="layout-2">
          <div>
            {mutationError && (
              <div className="alert err" role="alert" style={{ marginBottom: 'var(--s-4)' }}>
                {messageOf(mutationError)}
              </div>
            )}
            <ul className="lines">
              {cart.data.items.map((item) => {
                const over = item.quantity > item.availableStock;
                return (
                  <li className="line" key={item.id}>
                    <Link to={`/produk/${item.productId}`} aria-hidden="true" tabIndex={-1}>
                      <ProductVisual id={item.productId} categoryId={item.productId} name={item.productName} imageUrl={item.imageUrl} size="thumb" />
                    </Link>
                    <div className="body">
                      <Link className="name" to={`/produk/${item.productId}`}>
                        {item.productName}
                      </Link>
                      <span className="muted tnum">{formatRupiah(item.unitPrice)}</span>
                      {over && (
                        <span className="field-error" role="alert">
                          Stok tersisa {item.availableStock}. Kurangi jumlahnya untuk melanjutkan.
                        </span>
                      )}
                      <div className="controls">
                        <div className="stepper" role="group" aria-label={`Jumlah ${item.productName}`}>
                          <button type="button" aria-label="Kurangi jumlah" disabled={item.quantity <= 1} onClick={() => update.mutate({ itemId: item.id, quantity: item.quantity - 1 })}>
                            <span aria-hidden="true">&minus;</span>
                          </button>
                          <output className="tnum">{item.quantity}</output>
                          <button
                            type="button"
                            aria-label="Tambah jumlah"
                            disabled={item.quantity >= item.availableStock}
                            onClick={() => update.mutate({ itemId: item.id, quantity: clampQuantity(item.quantity + 1, item.availableStock) })}
                          >
                            <span aria-hidden="true">+</span>
                          </button>
                        </div>
                        <span className="sub">{formatRupiah(item.subtotal)}</span>
                        <button type="button" className="link-btn" onClick={() => remove.mutate({ itemId: item.id })} aria-label={`Hapus ${item.productName} dari keranjang`}>
                          Hapus
                        </button>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
          <aside>
            <Receipt
              title="Ringkasan"
              rows={[{ label: `Barang (${cart.data.totalQuantity})`, value: formatRupiah(cart.data.total) }]}
              total={cart.data.total}
              actions={
                <>
                  <button type="button" className="btn block" disabled={cart.data.items.some((i) => i.quantity > i.availableStock)} onClick={() => navigate('/checkout')}>
                    Lanjut ke checkout
                  </button>
                  <Link className="btn ghost block" to="/">
                    Lanjut belanja
                  </Link>
                </>
              }
            />
          </aside>
        </div>
      )}
    </div>
  );
}
