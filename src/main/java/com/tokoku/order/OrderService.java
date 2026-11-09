package com.tokoku.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.cart.Cart;
import com.tokoku.cart.CartItem;
import com.tokoku.cart.CartItemRepository;
import com.tokoku.cart.CartRepository;
import com.tokoku.common.ConflictException;
import com.tokoku.common.NotFoundException;
import com.tokoku.order.OrderDtos.*;
import com.tokoku.product.Product;
import com.tokoku.product.ProductRepository;

@Service
public class OrderService {

    private record Line(Long productId, String name, int quantity) {}

    private final OrderRepository orders;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;

    public OrderService(OrderRepository orders, CartRepository carts, CartItemRepository cartItems,
            ProductRepository products) {
        this.orders = orders;
        this.carts = carts;
        this.cartItems = cartItems;
        this.products = products;
    }

    /**
     * Checkout = satu transaksi: kurangi stok, buat order, kosongkan keranjang. Jika satu produk kurang stok,
     * exception membatalkan (rollback) SEMUA langkah, jadi tidak ada stok yang berkurang setengah-setengah.
     */
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest r) {
        Cart cart = carts.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
        // Salin data keranjang ke objek biasa dulu: query update di bawah membersihkan persistence context.
        List<Line> lines = new ArrayList<>(cartItems.findAllWithProduct(cart.getId()).stream()
                .map(i -> new Line(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity())).toList());
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        // Urutan tetap (id naik) supaya dua checkout bersamaan mengunci baris dengan urutan sama = tanpa deadlock
        lines.sort(Comparator.comparing(Line::productId));

        Order order = new Order(userId, r.shippingName().trim(), r.shippingPhone().trim(), r.shippingAddress().trim());
        for (Line line : lines) {
            // UPDATE ... WHERE stock >= qty bersifat atomik di DB; 0 baris berubah = stok tidak cukup
            if (products.decrementStock(line.productId(), line.quantity()) == 0) {
                throw new ConflictException("Insufficient stock or unavailable product: " + line.name());
            }
            Product p = products.findById(line.productId()).orElseThrow(); // harga terbaru, sesudah baris terkunci
            order.addItem(p.getId(), p.getName(), line.quantity(), p.getPrice());
        }
        orders.save(order);
        cartItems.deleteAllByCartId(cart.getId());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listMine(Long userId, Pageable pageable) {
        return orders.findByUserId(userId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(Long userId, Long orderId) {
        return OrderResponse.from(findOwned(userId, orderId));
    }

    // Order milik orang lain = 404, bukan 403, supaya keberadaan order tidak bocor
    Order findOwned(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listAll(OrderStatus status, Pageable pageable) {
        return orders.findAllByStatus(status, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getAny(Long orderId) {
        return OrderResponse.from(orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found")));
    }

    /** Admin hanya boleh memajukan pemenuhan order. PAID hanya lewat webhook; CANCELLED lewat pembatalan/kedaluwarsa. */
    @Transactional
    public OrderResponse adminChangeStatus(Long orderId, OrderStatus next) {
        if (next != OrderStatus.PROCESSING && next != OrderStatus.SHIPPED && next != OrderStatus.COMPLETED) {
            throw new ConflictException("Admin can only set PROCESSING, SHIPPED or COMPLETED");
        }
        Order o = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        o.transitionTo(next);
        return OrderResponse.from(o);
    }
}
