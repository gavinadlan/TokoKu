package com.tokoku.order;

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
}
