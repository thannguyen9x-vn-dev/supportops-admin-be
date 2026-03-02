package com.supportops.api.modules.user.repository;

import com.supportops.api.modules.user.entity.UserPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    Optional<UserPreference> findByUserIdAndTenantId(UUID userId, UUID tenantId);
}
