import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, call } from './client';
import type { CartResponse, CheckoutInput, ProductInput } from './types';
import { useAuth } from '../auth/AuthContext';
import { withQuantity, without, type CartTotals } from '../lib/cart';
import { apiSort, PAGE_SIZE, type CatalogParams } from '../lib/catalog';
import type { OrderStatus } from '../lib/status';

/* ---------- katalog (publik) ---------- */
export function useProducts(p: CatalogParams) {
  return useQuery({
    queryKey: ['products', p],
    placeholderData: keepPreviousData,
    queryFn: () =>
      call(() =>
        api.GET('/api/products', {
          params: { query: { q: p.q || undefined, category: p.category ?? undefined, page: p.page - 1, size: PAGE_SIZE, sort: [apiSort(p.sort)] } },
        }),
      ),
  });
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: ['product', id],
    enabled: Number.isFinite(id),
    retry: (count, error) => !(error instanceof Error && 'status' in error && error.status === 404) && count < 2,
    queryFn: () => call(() => api.GET('/api/products/{id}', { params: { path: { id } } })),
  });
}

export function useCategories() {
  return useQuery({ queryKey: ['categories'], staleTime: 60_000, queryFn: () => call(() => api.GET('/api/categories')) });
}

/* ---------- keranjang ---------- */
export function useCart() {
  const { status } = useAuth();
  return useQuery({ queryKey: ['cart'], enabled: status === 'authenticated', queryFn: () => call(() => api.GET('/api/cart')) });
}

export function useAddToCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { productId: number; quantity: number }) => call(() => api.POST('/api/cart/items', { body: v })),
    onSuccess: (cart) => qc.setQueryData(['cart'], cart),
  });
}

// Optimistic update: tampilan langsung berubah, dikembalikan bila server menolak (mis. stok kurang)
function useCartMutation<V>(apply: (cart: CartTotals, v: V) => CartTotals, request: (v: V) => Promise<CartResponse>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: request,
    onMutate: async (v: V) => {
      await qc.cancelQueries({ queryKey: ['cart'] });
      const previous = qc.getQueryData<CartResponse>(['cart']);
      if (previous) qc.setQueryData(['cart'], apply(previous as CartTotals, v));
      return { previous };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.previous) qc.setQueryData(['cart'], ctx.previous);
    },
    onSuccess: (cart) => qc.setQueryData(['cart'], cart),
    onSettled: () => qc.invalidateQueries({ queryKey: ['cart'] }),
  });
}

export function useUpdateCartItem() {
  return useCartMutation<{ itemId: number; quantity: number }>(
    (cart, v) => withQuantity(cart, v.itemId, v.quantity),
    (v) => call(() => api.PUT('/api/cart/items/{id}', { params: { path: { id: v.itemId } }, body: { quantity: v.quantity } })),
  );
}

export function useRemoveCartItem() {
  return useCartMutation<{ itemId: number }>(
    (cart, v) => without(cart, v.itemId),
    (v) => call(() => api.DELETE('/api/cart/items/{id}', { params: { path: { id: v.itemId } } })),
  );
}

/* ---------- order & pembayaran ---------- */
export function useCheckout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CheckoutInput) => call(() => api.POST('/api/orders', { body })),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cart'] });
      qc.invalidateQueries({ queryKey: ['orders'] });
      qc.invalidateQueries({ queryKey: ['products'] });
    },
  });
}

export function useOrders(page: number) {
  const { status } = useAuth();
  return useQuery({
    queryKey: ['orders', page],
    enabled: status === 'authenticated',
    placeholderData: keepPreviousData,
    queryFn: () => call(() => api.GET('/api/orders', { params: { query: { page: page - 1, size: 10 } } })),
  });
}

export function useOrder(id: number, opts: { poll?: boolean } = {}) {
  const { status } = useAuth();
  return useQuery({
    queryKey: ['order', id],
    enabled: status === 'authenticated' && Number.isFinite(id),
    refetchInterval: (q) => (opts.poll && q.state.data?.status === 'PENDING_PAYMENT' ? 4000 : false),
    queryFn: () => call(() => api.GET('/api/orders/{id}', { params: { path: { id } } })),
  });
}

export function usePay() {
  return useMutation({ mutationFn: (orderId: number) => call(() => api.POST('/api/orders/{id}/pay', { params: { path: { id: orderId } } })) });
}

export function useCancelOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) => call(() => api.POST('/api/orders/{id}/cancel', { params: { path: { id: orderId } } })),
    onSuccess: (order) => {
      qc.setQueryData(['order', order.id], order);
      qc.invalidateQueries({ queryKey: ['orders'] });
      qc.invalidateQueries({ queryKey: ['products'] });
    },
  });
}

/* ---------- admin ---------- */
export function useAdminProducts(p: { q: string; active: boolean | null; page: number }) {
  return useQuery({
    queryKey: ['admin', 'products', p],
    placeholderData: keepPreviousData,
    queryFn: () =>
      call(() => api.GET('/api/admin/products', { params: { query: { q: p.q || undefined, active: p.active ?? undefined, page: p.page - 1, size: 15, sort: ['id,desc'] } } })),
  });
}

function invalidateCatalog(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['admin'] });
  qc.invalidateQueries({ queryKey: ['products'] });
  qc.invalidateQueries({ queryKey: ['product'] });
  qc.invalidateQueries({ queryKey: ['categories'] });
}

export function useSaveProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id?: number; body: ProductInput }) =>
      v.id
        ? call(() => api.PUT('/api/products/{id}', { params: { path: { id: v.id! } }, body: v.body }))
        : call(() => api.POST('/api/products', { body: v.body })),
    onSuccess: () => invalidateCatalog(qc),
  });
}

export function useDeactivateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => call(() => api.DELETE('/api/products/{id}', { params: { path: { id } } })),
    onSuccess: () => invalidateCatalog(qc),
  });
}

export function useSaveCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id?: number; name: string }) =>
      v.id
        ? call(() => api.PUT('/api/categories/{id}', { params: { path: { id: v.id! } }, body: { name: v.name } }))
        : call(() => api.POST('/api/categories', { body: { name: v.name } })),
    onSuccess: () => invalidateCatalog(qc),
  });
}

export function useDeleteCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => call(() => api.DELETE('/api/categories/{id}', { params: { path: { id } } })),
    onSuccess: () => invalidateCatalog(qc),
  });
}

export function useAdminOrders(p: { status: OrderStatus | null; page: number }) {
  return useQuery({
    queryKey: ['admin', 'orders', p],
    placeholderData: keepPreviousData,
    queryFn: () =>
      call(() => api.GET('/api/admin/orders', { params: { query: { status: p.status ?? undefined, page: p.page - 1, size: 15, sort: ['createdAt,desc'] } } })),
  });
}

export function useAdminOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; status: 'PROCESSING' | 'SHIPPED' | 'COMPLETED' }) =>
      call(() => api.PATCH('/api/admin/orders/{id}/status', { params: { path: { id: v.id } }, body: { status: v.status } })),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'orders'] }),
  });
}
