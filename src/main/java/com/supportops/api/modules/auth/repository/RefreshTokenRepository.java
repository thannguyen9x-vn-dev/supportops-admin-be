package com.supportops.api.modules.auth.repository;

import com.supportops.api.modules.auth.entity.RefreshToken;
import com.supportops.api.modules.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    void deleteByUser(User user);
}
