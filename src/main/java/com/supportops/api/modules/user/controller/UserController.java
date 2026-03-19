package com.supportops.api.modules.user.controller;

import com.supportops.api.common.dto.ApiResponse;
import com.supportops.api.common.exception.AppException;
import com.supportops.api.common.exception.NotFoundException;
import com.supportops.api.common.exception.ValidationException;
import com.supportops.api.common.security.CurrentUser;
import com.supportops.api.common.storage.ObjectStorageService;
import com.supportops.api.modules.user.dto.AvatarUploadResponse;
import com.supportops.api.modules.user.dto.ChangePasswordRequest;
import com.supportops.api.modules.user.dto.UpdateUserProfileRequest;
import com.supportops.api.modules.user.dto.UpdateUserPreferencesRequest;
import com.supportops.api.modules.user.dto.UserProfileResponse;
import com.supportops.api.modules.user.dto.UserPreferencesResponse;
import com.supportops.api.modules.user.entity.User;
import com.supportops.api.modules.user.entity.UserPreference;
import com.supportops.api.modules.user.repository.UserPreferenceRepository;
import com.supportops.api.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final long MAX_AVATAR_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_LOCALE = "en";

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectStorageService objectStorageService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        CurrentUser principal = resolveCurrentUser(currentUser);
        User user = userRepository.findByIdAndTenantId(principal.userId(), principal.tenantId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        return ApiResponse.of(toUserProfileResponse(user));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        CurrentUser principal = resolveCurrentUser(currentUser);
        User user = userRepository.findByIdAndTenantId(principal.userId(), principal.tenantId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.firstName() != null) user.setFirstName(trimToNull(request.firstName()));
        if (request.lastName() != null) user.setLastName(trimToNull(request.lastName()));
        if (request.phone() != null) user.setPhone(trimToNull(request.phone()));
        if (request.birthday() != null) user.setBirthday(trimToNull(request.birthday()));
        if (request.address() != null) user.setAddress(trimToNull(request.address()));
        if (request.city() != null) user.setCity(trimToNull(request.city()));
        if (request.zipCode() != null) user.setZipCode(trimToNull(request.zipCode()));
        if (request.country() != null) user.setCountry(trimToNull(request.country()));
        if (request.organization() != null) user.setOrganization(trimToNull(request.organization()));
        if (request.department() != null) user.setDepartment(trimToNull(request.department()));
        if (request.timezone() != null) user.setTimezone(defaultIfBlank(request.timezone(), DEFAULT_TIMEZONE));
        if (request.locale() != null) user.setLocale(defaultIfBlank(request.locale(), DEFAULT_LOCALE));

        if (isBlank(user.getFirstName()) || isBlank(user.getLastName())) {
            throw new ValidationException("First name and last name are required");
        }

        User saved = userRepository.save(user);
        return ApiResponse.of(toUserProfileResponse(saved));
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

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarUploadResponse> uploadAvatar(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestParam("file") MultipartFile file
    ) {
        CurrentUser principal = resolveCurrentUser(currentUser);

        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "MISSING_FILE", "Avatar file is required");
        }

        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Avatar file size exceeds 2 MB");
        }

        String contentType = normalize(file.getContentType());
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_TYPE", "Only JPG, PNG, WEBP are supported");
        }

        User user = userRepository.findByIdAndTenantId(principal.userId(), principal.tenantId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        String objectKey = buildAvatarObjectKey(principal.tenantId(), principal.userId(), file.getOriginalFilename());
        String avatarUrl = uploadAvatarObject(objectKey, file);

        String previousAvatarUrl = normalize(user.getAvatarUrl());
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        if (previousAvatarUrl != null && !previousAvatarUrl.equals(avatarUrl)) {
            objectStorageService.deleteObjectByUrl(previousAvatarUrl);
        }

        return ApiResponse.of(new AvatarUploadResponse(avatarUrl));
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

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getAvatarUrl(),
            user.getBirthday(),
            user.getAddress(),
            user.getCity(),
            user.getZipCode(),
            user.getCountry(),
            user.getOrganization(),
            user.getDepartment(),
            defaultIfBlank(user.getTimezone(), DEFAULT_TIMEZONE),
            defaultIfBlank(user.getLocale(), DEFAULT_LOCALE),
            user.getRole(),
            user.getTenantId(),
            user.getTenantName()
        );
    }

    private String buildAvatarObjectKey(UUID tenantId, UUID userId, String originalFilename) {
        String fileName = normalize(originalFilename);
        if (fileName == null) {
            fileName = "avatar";
        }

        String safeFileName = fileName
            .replaceAll("[^a-zA-Z0-9._-]", "-")
            .replaceAll("-+", "-");

        return "avatars/" + tenantId + "/" + userId + "/" + UUID.randomUUID() + "-" + safeFileName;
    }

    private String uploadAvatarObject(String objectKey, MultipartFile file) {
        try {
            return objectStorageService.uploadPublicObject(objectKey, file.getBytes(), file.getContentType());
        } catch (IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_FILE_CONTENT", "Unable to read avatar content");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToNull(String value) {
        return normalize(value);
    }

    private boolean isBlank(String value) {
        return normalize(value) == null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized != null ? normalized : defaultValue;
    }
}
