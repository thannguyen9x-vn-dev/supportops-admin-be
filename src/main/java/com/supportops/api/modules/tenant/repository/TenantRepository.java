package com.supportops.api.modules.tenant.repository;

import com.supportops.api.modules.tenant.entity.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySlug(String slug);
}
