package com.tokoku.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select i from CartItem i join fetch i.product where i.cart.id = :cartId order by i.id")
    List<CartItem> findAllWithProduct(@Param("cartId") Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @Query("select i from CartItem i join fetch i.product where i.id = :id and i.cart.userId = :userId")
    Optional<CartItem> findOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem i where i.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}
