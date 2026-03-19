package com.supportops.api.modules.user.dto;

import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    String avatarUrl,
    String birthday,
    String address,
    String city,
    String zipCode,
    String country,
    String organization,
    String department,
    String timezone,
    String locale,
    String role,
    UUID tenantId,
    String tenantName
) {
}
