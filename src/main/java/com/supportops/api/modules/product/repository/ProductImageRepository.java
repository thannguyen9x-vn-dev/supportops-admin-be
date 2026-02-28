package com.supportops.api.modules.product.repository;

import com.supportops.api.modules.product.entity.ProductImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    @Query("""
        SELECT pi
        FROM ProductImage pi
        WHERE pi.id = :imageId
          AND pi.product.id = :productId
          AND pi.product.tenantId = :tenantId
        """)
    Optional<ProductImage> findScopedById(@Param("imageId") UUID imageId,
                                          @Param("productId") UUID productId,
                                          @Param("tenantId") UUID tenantId);

    @Query("""
        SELECT pi
        FROM ProductImage pi
        WHERE pi.product.id = :productId
          AND pi.product.tenantId = :tenantId
        ORDER BY pi.sortOrder ASC
        """)
    List<ProductImage> findByProductIdAndTenantIdOrderBySortOrder(@Param("productId") UUID productId,
                                                                   @Param("tenantId") UUID tenantId);
}
