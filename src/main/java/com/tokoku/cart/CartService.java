package com.tokoku.cart;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.cart.CartDtos.*;
import com.tokoku.common.ConflictException;
import com.tokoku.common.NotFoundException;
import com.tokoku.product.Product;
import com.tokoku.product.ProductImageUrls;
import com.tokoku.product.ProductRepository;

@Service
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final ProductImageUrls urls;

    public CartService(CartRepository carts, CartItemRepository items, ProductRepository products, ProductImageUrls urls) {
        this.urls = urls;
        this.carts = carts;
        this.items = items;
        this.products = products;
    }

    @Transactional
    public CartResponse get(Long userId) {
        return toResponse(findOrCreate(userId));
    }

    @Transactional
    public CartResponse add(Long userId, AddItemRequest r) {
        Cart cart = findOrCreate(userId);
        Product product = products.findByIdAndActiveTrue(r.productId())
                .orElseThrow(() -> new NotFoundException("Product " + r.productId() + " not found"));
        CartItem item = items.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);
        int newQty = (item == null ? 0 : item.getQuantity()) + r.quantity();
        ensureStock(product, newQty);
        if (item == null) {
            items.save(new CartItem(cart, product, newQty));
        } else {
            item.setQuantity(newQty); // menambah produk yang sama = menambah jumlah
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse update(Long userId, Long itemId, UpdateItemRequest r) {
        CartItem item = items.findOwned(itemId, userId).orElseThrow(() -> new NotFoundException("Cart item not found"));
        if (!item.getProduct().isActive()) {
            throw new ConflictException("Product is no longer available");
        }
        ensureStock(item.getProduct(), r.quantity());
        item.setQuantity(r.quantity());
        return toResponse(item.getCart());
    }

    @Transactional
    public CartResponse remove(Long userId, Long itemId) {
        CartItem item = items.findOwned(itemId, userId).orElseThrow(() -> new NotFoundException("Cart item not found"));
        Cart cart = item.getCart();
        items.delete(item);
        items.flush();
        return toResponse(cart);
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new ConflictException("Insufficient stock for " + product.getName() + " (available " + product.getStock() + ")");
        }
    }

    private Cart findOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(userId)));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> list = items.findAllWithProduct(cart.getId()).stream().map(i -> {
            Product p = i.getProduct();
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CartItemResponse(i.getId(), p.getId(), p.getCategory().getId(), p.getName(), urls.primaryThumbnail(p), p.getPrice(),
                    i.getQuantity(), subtotal, p.getStock());
        }).toList();
        BigDecimal total = list.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(list, list.stream().mapToInt(CartItemResponse::quantity).sum(), total);
    }
}
