package com.supportops.api.modules.product.repository;

import com.supportops.api.modules.product.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT p
        FROM Product p
        WHERE p.tenantId = :tenantId
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(p.subtitle, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (
            :category IS NULL OR :category = ''
            OR LOWER(p.category) = LOWER(:category)
          )
        """)
    Page<Product> search(
        @Param("tenantId") UUID tenantId,
        @Param("search") String search,
        @Param("category") String category,
        Pageable pageable
    );

    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.id IN :ids")
    List<Product> findAllByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);
}
