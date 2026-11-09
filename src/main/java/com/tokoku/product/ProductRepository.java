package com.tokoku.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // pattern sudah berbentuk %kata% huruf kecil, supaya tidak ada parameter String null
    @Query("""
            select p from Product p
            where p.active = true
              and lower(p.name) like :pattern
              and (:categoryId is null or p.category.id = :categoryId)
            """)
    Page<Product> search(@Param("pattern") String pattern, @Param("categoryId") Long categoryId, Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);
}
