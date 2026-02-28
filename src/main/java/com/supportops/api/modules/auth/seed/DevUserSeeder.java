package com.supportops.api.modules.auth.seed;

import com.supportops.api.modules.tenant.entity.Tenant;
import com.supportops.api.modules.tenant.repository.TenantRepository;
import com.supportops.api.modules.user.entity.User;
import com.supportops.api.modules.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "dev-user-enabled", havingValue = "true")
public class DevUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.seed.dev-user-email:admin@supportops.local}")
    private String seedEmail;

    @org.springframework.beans.factory.annotation.Value("${app.seed.dev-user-password:Admin@12345}")
    private String seedPassword;

    @org.springframework.beans.factory.annotation.Value("${app.seed.dev-user-first-name:Support}")
    private String seedFirstName;

    @org.springframework.beans.factory.annotation.Value("${app.seed.dev-user-last-name:Admin}")
    private String seedLastName;

    @org.springframework.beans.factory.annotation.Value("${app.seed.dev-tenant-name:SupportOps Dev}")
    private String seedTenantName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(seedEmail)) {
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setName(seedTenantName.trim());
        tenant.setSlug(generateTenantSlug(seedTenantName));
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setEmail(seedEmail);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        user.setFirstName(seedFirstName);
        user.setLastName(seedLastName);
        user.setRole("ADMIN");
        user.setTenantId(tenant.getId());
        user.setTenantName(tenant.getName());
        user.setActive(true);

        userRepository.save(user);
        log.info("Seeded dev user: email={}", seedEmail);
    }

    private String generateTenantSlug(String tenantName) {
        String baseSlug = tenantName
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (baseSlug.isBlank()) {
            baseSlug = "tenant";
        }

        String candidate = baseSlug;
        int suffix = 1;
        while (tenantRepository.existsBySlug(candidate)) {
            suffix++;
            candidate = baseSlug + "-" + suffix;
        }
        return candidate;
    }
}
