package com.supportops.api.modules.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 10, max = 32, message = "Password must be between 10 and 32 characters")
    @Pattern(regexp = ".*[a-z].*", message = "At least one lowercase character")
    @Pattern(regexp = ".*[A-Z].*", message = "At least one uppercase character")
    @Pattern(regexp = ".*[0-9].*", message = "At least one number")
    @Pattern(regexp = ".*[!@#$%^&*(),.?\":{}|<>].*", message = "At least one special character")
    String newPassword,

    @NotBlank(message = "Confirm password is required")
    String confirmPassword
) {

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
