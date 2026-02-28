package com.supportops.api.modules.user.controller;

import com.supportops.api.common.dto.ApiResponse;
import com.supportops.api.common.exception.NotFoundException;
import com.supportops.api.common.security.CurrentUser;
import com.supportops.api.modules.user.dto.AuthUserResponse;
import com.supportops.api.modules.user.entity.User;
import com.supportops.api.modules.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            throw new NotFoundException("User not found");
        }

        UUID userId = currentUser.userId();
        if (userId == null) {
            throw new NotFoundException("User not found");
        }

        User user = userRepository.findById(userId)
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
}
