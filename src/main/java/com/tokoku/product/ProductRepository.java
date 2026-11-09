package com.tokoku.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            select p from Product p
            where lower(p.name) like :pattern
              and (:categoryId is null or p.category.id = :categoryId)
              and (:active is null or p.active = :active)
            """)
    Page<Product> searchAll(@Param("pattern") String pattern, @Param("categoryId") Long categoryId,
            @Param("active") Boolean active, Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    // Kurangi stok atomik di DB: baris hanya berubah jika stok cukup, sehingga stok tidak pernah negatif
    // walau banyak checkout bersamaan. Mengembalikan jumlah baris yang berubah (0 = gagal).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty, p.version = p.version + 1 "
            + "where p.id = :id and p.active = true and p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :qty, p.version = p.version + 1 where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}
