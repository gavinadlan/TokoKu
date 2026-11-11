import { Link } from 'react-router';
import type { Product } from '../api/types';
import { formatRupiah } from '../lib/format';
import { ProductVisual } from './ProductVisual';

export function StockNote({ stock }: { stock: number }) {
  if (stock <= 0) return <span className="stock-note out">Habis</span>;
  if (stock <= 5) return <span className="stock-note low">Sisa {stock}</span>;
  return null;
}

export function ProductCard({ product, showCategory = true }: { product: Product; showCategory?: boolean }) {
  return (
    <Link className="card" to={`/produk/${product.id}`} data-out={product.stock <= 0}>
      <ProductVisual id={product.id} categoryId={product.categoryId} name={product.name} images={product.images} />
      <div className="info">
        {showCategory && <span className="cat">{product.categoryName}</span>}
        <span className="name">{product.name}</span>
        <div className="meta-row">
          <span className="price">{formatRupiah(product.price)}</span>
          <StockNote stock={product.stock} />
        </div>
      </div>
    </Link>
  );
}
