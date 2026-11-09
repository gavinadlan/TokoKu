import type { components } from './schema';

type S = components['schemas'];
export type Product = S['ProductResponse'];
export type ProductInput = S['ProductRequest'];
export type Category = S['CategoryResponse'];
export type CartResponse = S['CartResponse'];
export type CartItem = S['CartItemResponse'];
export type Order = S['OrderResponse'];
export type OrderItem = S['OrderItemResponse'];
export type PayResponse = S['PayResponse'];
export type TokenResponse = S['TokenResponse'];
export type User = S['UserResponse'];
export type CheckoutInput = S['CheckoutRequest'];
export type ProductPage = S['PagedModelProductResponse'];
export type OrderPage = S['PagedModelOrderResponse'];
