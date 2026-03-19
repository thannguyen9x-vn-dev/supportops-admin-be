package com.supportops.api.modules.user.graphql;

import com.supportops.api.common.exception.UnauthorizedException;
import com.supportops.api.common.security.CurrentUser;
import com.supportops.api.modules.user.dto.UserPreferencesResponse;
import com.supportops.api.modules.user.entity.UserPreference;
import com.supportops.api.modules.user.repository.UserPreferenceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserGraphqlController {

    private final UserPreferenceRepository userPreferenceRepository;

    @QueryMapping
    public UserPreferencesResponse meSettings(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        UserPreference preferences = getOrCreatePreferences(currentUser.userId(), currentUser.tenantId());

        return new UserPreferencesResponse(
            preferences.isCompanyNews(),
            preferences.isAccountActivity(),
            preferences.isMeetupsNearYou(),
            preferences.isNewMessages(),
            preferences.isRatingReminders(),
            preferences.isItemUpdateNotif(),
            preferences.isItemCommentNotif(),
            preferences.isBuyerReviewNotif()
        );
    }

    private UserPreference getOrCreatePreferences(UUID userId, UUID tenantId) {
        return userPreferenceRepository.findByUserIdAndTenantId(userId, tenantId)
            .orElseGet(() -> {
                UserPreference userPreference = new UserPreference();
                userPreference.setUserId(userId);
                userPreference.setTenantId(tenantId);
                return userPreferenceRepository.save(userPreference);
            });
    }
}
