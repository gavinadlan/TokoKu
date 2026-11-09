package com.tokoku.order;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.tokoku.payment.PaymentRepository;
import com.tokoku.product.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Service
public class OrderService {

    private record Line(Long productId, String name, int quantity) {}

    private final OrderRepository orders;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final PaymentRepository payments;
    private final Duration expiry;

    @PersistenceContext
    private EntityManager em;

    public OrderService(OrderRepository orders, CartRepository carts, CartItemRepository cartItems,
            ProductRepository products, PaymentRepository payments,
            @Value("${app.order.expiry-minutes:60}") long expiryMinutes) {
        this.orders = orders;
        this.carts = carts;
        this.cartItems = cartItems;
        this.products = products;
        this.payments = payments;
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    private Instant cutoff() { return Instant.now().minus(expiry); }

    /**
     * Checkout = satu transaksi: kurangi stok, buat order, kosongkan keranjang. Jika satu produk kurang stok,
     * exception membatalkan (rollback) SEMUA langkah, jadi tidak ada stok yang berkurang setengah-setengah.
     */
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest r) {
        expireOverdue(); // kedaluwarsa "malas": stok dari order basi dilepas dulu agar checkout ini bisa memakainya
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

    // Bukan readOnly: membaca order yang sudah lewat batas waktu akan membatalkannya (lihat expireIfOverdue)
    @Transactional
    public Page<OrderResponse> listMine(Long userId, Pageable pageable) {
        Page<Order> page = orders.findByUserId(userId, pageable);
        page.forEach(this::expireIfOverdue);
        return page.map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse getMine(Long userId, Long orderId) {
        Order o = findOwned(userId, orderId);
        expireIfOverdue(o);
        return OrderResponse.from(o);
    }

    /** Pemilik membatalkan order yang belum dibayar; stok kembali. */
    @Transactional
    public OrderResponse cancelMine(Long userId, Long orderId) {
        Order o = orders.findOwnedForUpdate(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Only PENDING_PAYMENT orders can be cancelled (status is " + o.getStatus() + ")");
        }
        cancelAndRestock(o);
        return OrderResponse.from(o);
    }

    /**
     * Pembatalan otomatis order yang melewati batas waktu. Dijalankan oleh scheduler DAN secara "malas" saat order
     * dibaca / checkout, karena di hosting gratis aplikasi tidur dan scheduler tidak berjalan.
     */
    @Transactional
    public int expireOverdue() {
        List<Order> overdue = orders.findOverdueForUpdate(cutoff(), PageRequest.of(0, 100));
        overdue.forEach(this::cancelAndRestock);
        return overdue.size();
    }

    /** Dipanggil untuk order yang sudah dimuat tanpa kunci: muat ulang dengan kunci lalu batalkan jika memang basi. */
    public boolean expireIfOverdue(Order o) {
        if (!o.isOverdue(cutoff())) {
            return false;
        }
        em.refresh(o, LockModeType.PESSIMISTIC_WRITE); // status terbaru di bawah kunci
        return cancelIfOverdue(o);
    }

    /** Pemanggil sudah memegang kunci baris order. */
    public boolean cancelIfOverdue(Order lockedOrder) {
        if (!lockedOrder.isOverdue(cutoff())) {
            return false;
        }
        cancelAndRestock(lockedOrder);
        return true;
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

    /**
     * Membatalkan order yang belum dibayar dan mengembalikan stok. Dipanggil dari webhook, pembatalan, dan kedaluwarsa.
     * Harus dipanggil di dalam transaksi pemanggil, dan sebaiknya sesudah semua perubahan entity lain
     * (update stok memakai flush otomatis).
     */
    public void cancelAndRestock(Order order) {
        order.transitionTo(OrderStatus.CANCELLED);
        payments.closePending(order.getId());
        order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductId)) // urutan tetap = tanpa deadlock
                .forEach(i -> products.incrementStock(i.getProductId(), i.getQuantity()));
    }
}
