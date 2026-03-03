package com.supportops.api.modules.file.controller;

import com.supportops.api.common.dto.ApiResponse;
import com.supportops.api.common.exception.ValidationException;
import com.supportops.api.common.storage.ObjectStorageService;
import com.supportops.api.modules.file.dto.FileAccessUrlResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private static final long MIN_EXPIRES_SECONDS = 60L;
    private static final long MAX_EXPIRES_SECONDS = 3600L;

    private final ObjectStorageService objectStorageService;

    @GetMapping("/access-url")
    public ApiResponse<FileAccessUrlResponse> getAccessUrl(
        @RequestParam String url,
        @RequestParam(defaultValue = "300") long expiresInSeconds
    ) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("url is required");
        }
        if (expiresInSeconds < MIN_EXPIRES_SECONDS || expiresInSeconds > MAX_EXPIRES_SECONDS) {
            throw new ValidationException("expiresInSeconds must be between 60 and 3600");
        }

        String signedUrl = objectStorageService.createTemporaryReadUrlFromUrl(url, expiresInSeconds);
        FileAccessUrlResponse response = new FileAccessUrlResponse(
            signedUrl,
            Instant.now().plusSeconds(expiresInSeconds)
        );

        return ApiResponse.of(response);
    }
}
