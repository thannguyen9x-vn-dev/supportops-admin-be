package com.supportops.api.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 10, message = "At least 10 characters")
    @Pattern(regexp = ".*[a-z].*", message = "At least one lowercase character")
    @Pattern(regexp = ".*[!@#$%^&*(),.?\":{}|<>].*", message = "At least one special character")
    String password,

    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName,

    @NotBlank(message = "Organization is required")
    String organizationName
) {}
