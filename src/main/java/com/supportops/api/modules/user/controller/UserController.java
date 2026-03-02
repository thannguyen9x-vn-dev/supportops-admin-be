package com.supportops.api.modules.user.controller;

import com.supportops.api.common.dto.ApiResponse;
import com.supportops.api.common.exception.NotFoundException;
import com.supportops.api.common.exception.ValidationException;
import com.supportops.api.common.security.CurrentUser;
import com.supportops.api.modules.user.dto.AuthUserResponse;
import com.supportops.api.modules.user.dto.ChangePasswordRequest;
import com.supportops.api.modules.user.dto.UpdateUserPreferencesRequest;
import com.supportops.api.modules.user.dto.UserPreferencesResponse;
import com.supportops.api.modules.user.entity.User;
import com.supportops.api.modules.user.entity.UserPreference;
import com.supportops.api.modules.user.repository.UserPreferenceRepository;
import com.supportops.api.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        CurrentUser principal = resolveCurrentUser(currentUser);
        User user = userRepository.findById(principal.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        AuthUserResponse response = new AuthUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAvatarUrl(),
            user.getRole(),
            user.getTenantId(),
            user.getTenantName()
        );

        return ApiResponse.of(response);
    }

    @GetMapping("/me/preferences")
    public ApiResponse<UserPreferencesResponse> getPreferences(@AuthenticationPrincipal CurrentUser currentUser) {
        CurrentUser principal = resolveCurrentUser(currentUser);
        UserPreference preferences = getOrCreatePreferences(principal);
        return ApiResponse.of(toResponse(preferences));
    }

    @PutMapping("/me/preferences")
    public ApiResponse<UserPreferencesResponse> updatePreferences(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestBody UpdateUserPreferencesRequest request
    ) {
        CurrentUser principal = resolveCurrentUser(currentUser);
        UserPreference preferences = getOrCreatePreferences(principal);

        if (request.companyNews() != null) preferences.setCompanyNews(request.companyNews());
        if (request.accountActivity() != null) preferences.setAccountActivity(request.accountActivity());
        if (request.meetupsNearYou() != null) preferences.setMeetupsNearYou(request.meetupsNearYou());
        if (request.newMessages() != null) preferences.setNewMessages(request.newMessages());
        if (request.ratingReminders() != null) preferences.setRatingReminders(request.ratingReminders());
        if (request.itemUpdateNotif() != null) preferences.setItemUpdateNotif(request.itemUpdateNotif());
        if (request.itemCommentNotif() != null) preferences.setItemCommentNotif(request.itemCommentNotif());
        if (request.buyerReviewNotif() != null) preferences.setBuyerReviewNotif(request.buyerReviewNotif());

        UserPreference saved = userPreferenceRepository.save(preferences);
        return ApiResponse.of(toResponse(saved));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        CurrentUser principal = resolveCurrentUser(currentUser);
        User user = userRepository.findById(principal.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ApiResponse.of(null);
    }

    private CurrentUser resolveCurrentUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.tenantId() == null) {
            throw new NotFoundException("User not found");
        }
        return currentUser;
    }

    private UserPreference getOrCreatePreferences(CurrentUser currentUser) {
        return userPreferenceRepository.findByUserIdAndTenantId(currentUser.userId(), currentUser.tenantId())
            .orElseGet(() -> {
                UserPreference userPreference = new UserPreference();
                userPreference.setUserId(currentUser.userId());
                userPreference.setTenantId(currentUser.tenantId());
                return userPreferenceRepository.save(userPreference);
            });
    }

    private UserPreferencesResponse toResponse(UserPreference preferences) {
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
}
