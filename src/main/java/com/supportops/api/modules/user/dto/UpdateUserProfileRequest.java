package com.supportops.api.modules.user.dto;

import com.supportops.api.common.validation.ValidPhoneNumber;

public record UpdateUserProfileRequest(
    String firstName,
    String lastName,
    @ValidPhoneNumber
    String phone,
    String birthday,
    String address,
    String city,
    String zipCode,
    String country,
    String organization,
    String department,
    String timezone,
    String locale
) {
}
