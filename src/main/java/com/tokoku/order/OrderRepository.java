package com.tokoku.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Kunci baris order (SELECT ... FOR UPDATE) agar dua request /pay bersamaan diproses bergantian
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.userId = :userId")
    Optional<Order> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Query("select o from Order o where (:status is null or o.status = :status)")
    Page<Order> findAllByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    // Dikunci supaya scheduler dan pembacaan "malas" tidak membatalkan order yang sama dua kali (stok kembali dobel)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.status = com.tokoku.order.OrderStatus.PENDING_PAYMENT and o.createdAt < :cutoff order by o.id")
    List<Order> findOverdueForUpdate(@Param("cutoff") Instant cutoff, Pageable pageable);

    // Polling admin: murah, memakai idx_orders_status_created dan idx_orders_invoiced_at
    long countByStatus(OrderStatus status);

    @Query("select count(o) from Order o where o.invoicedAt > :since")
    long countPaidSince(@Param("since") Instant since);
}
