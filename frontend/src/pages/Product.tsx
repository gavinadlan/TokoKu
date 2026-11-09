import { useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { useAddToCart, useCart, useProduct } from '../api/queries';
import { useAuth } from '../auth/AuthContext';
import { ProductVisual } from '../components/ProductVisual';
import { ErrorState } from '../components/States';
import { useToast } from '../components/Toast';
import { ApiError, messageOf } from '../lib/errors';
import { formatRupiah, productNumber } from '../lib/format';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { NotFoundView } from './NotFound';

function ProductSkeleton() {
  return (
    <div className="wrap detail" aria-busy="true" aria-label="Memuat produk" style={{ paddingTop: 'var(--s-6)' }}>
      <div className="skeleton vis-s" />
      <div>
        <div className="skeleton line-s short" />
        <div className="skeleton line-s" style={{ height: 48 }} />
        <div className="skeleton line-s short" />
      </div>
    </div>
  );
}

export function ProductPage() {
  const id = Number(useParams().id);
  const product = useProduct(id);
  const { status } = useAuth();
  const cart = useCart();
  const add = useAddToCart();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [qty, setQty] = useState(1);
  const [error, setError] = useState<string | null>(null);
  useDocumentTitle(product.data?.name);

  if (product.isPending) return <ProductSkeleton />;
  if (product.isError) {
    if (product.error instanceof ApiError && product.error.status === 404) return <NotFoundView title="Produk tidak ditemukan" message="Produk ini sudah tidak dijual atau alamatnya salah." />;
    return <div className="wrap"><ErrorState error={product.error} onRetry={() => product.refetch()} title="Produk belum bisa dimuat" /></div>;
  }

  const p = product.data;
  const out = p.stock <= 0;
  const inCart = cart.data?.items.find((i) => i.productId === p.id)?.quantity ?? 0;
  const maxAddable = Math.max(0, p.stock - inCart);
  const quantity = Math.min(qty, Math.max(1, maxAddable));

  const onAdd = () => {
    setError(null);
    if (status !== 'authenticated') {
      navigate('/masuk', { state: { from: location.pathname, notice: 'Masuk dulu untuk menyimpan barang di keranjang.' } });
      return;
    }
    add.mutate(
      { productId: p.id, quantity },
      {
        onSuccess: () => {
          setQty(1);
          toast.notify(`${p.name} masuk keranjang.`, <Link to="/keranjang">Lihat keranjang</Link>);
        },
        onError: (e) => setError(messageOf(e)),
      },
    );
  };

  const buyDisabled = out || maxAddable === 0;
  const buyLabel = out ? 'Stok habis' : maxAddable === 0 ? 'Semua stok sudah di keranjang' : add.isPending ? 'Menambahkan...' : 'Tambah ke keranjang';

  return (
    <div className="wrap">
      <nav className="crumbs" aria-label="Jejak halaman">
        <Link to="/">Katalog</Link> / <Link to={`/?kategori=${p.categoryId}`}>{p.categoryName}</Link> / <span aria-current="page">{p.name}</span>
      </nav>
      <div className="detail">
        <ProductVisual id={p.id} categoryId={p.categoryId} name={p.name} imageUrl={p.imageUrl} size="big" />
        <section className="panel" aria-labelledby="judul">
          <span className="cat">{p.categoryName}</span>
          <h1 id="judul" className="page-title">
            {p.name}
          </h1>
          <div className="price-big">{formatRupiah(p.price)}</div>
          {p.description && <p className="desc">{p.description}</p>}
          <span className={`avail ${out ? 'out' : p.stock <= 5 ? 'low' : ''}`}>
            {out ? 'Stok habis' : p.stock <= 5 ? `Sisa ${p.stock}, segera habis` : `Tersedia, ${p.stock} di stok`}
          </span>
          <div className="buy">
            {!buyDisabled && (
              <div className="qty-row">
                <span id="qty-label">Jumlah</span>
                <div className="stepper" role="group" aria-labelledby="qty-label">
                  <button type="button" aria-label="Kurangi jumlah" disabled={quantity <= 1} onClick={() => setQty(quantity - 1)}>
                    &minus;
                  </button>
                  <output className="tnum" aria-live="polite">
                    {quantity}
                  </output>
                  <button type="button" aria-label="Tambah jumlah" disabled={quantity >= maxAddable} onClick={() => setQty(quantity + 1)}>
                    +
                  </button>
                </div>
              </div>
            )}
            {inCart > 0 && !out && <p className="muted tiny-note">Sudah {inCart} di keranjang Anda.</p>}
            {error && (
              <div className="alert err" role="alert">
                {error}
              </div>
            )}
            <button type="button" className="btn" disabled={buyDisabled || add.isPending} onClick={onAdd}>
              {buyLabel}
            </button>
          </div>
          <dl className="facts">
            <div><dt>Nomor produk</dt><dd>{productNumber(p.id)}</dd></div>
            <div><dt>Kategori</dt><dd>{p.categoryName}</dd></div>
            <div><dt>Ketersediaan</dt><dd>{out ? 'Habis' : `${p.stock} tersedia`}</dd></div>
          </dl>
        </section>
      </div>
      <div className="buybar">
        <div className="p">
          <small>Harga</small>
          <b>{formatRupiah(p.price)}</b>
        </div>
        <button type="button" className="btn" disabled={buyDisabled || add.isPending} onClick={onAdd}>
          {buyLabel}
        </button>
      </div>
    </div>
  );
}
