package com.tokoku.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    boolean existsByPublicId(String publicId);

    // Dua langkah: indeks unik parsial dicek per baris, jadi foto utama lama harus dilepas dulu
    @Modifying(flushAutomatically = true)
    @Query("update ProductImage i set i.primary = false where i.product.id = :productId and i.primary = true")
    int clearPrimary(@Param("productId") Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductImage i set i.primary = true where i.id = :imageId")
    int markPrimary(@Param("imageId") Long imageId);
}
