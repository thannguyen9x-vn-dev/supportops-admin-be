package com.supportops.api.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "false")
public class NoopObjectStorageService implements ObjectStorageService {

    @Override
    public String uploadPublicObject(String key, byte[] data, String contentType) {
        return "/uploads/" + key;
    }

    @Override
    public void deleteObjectByUrl(String url) {
        // no-op for disabled storage
    }

    @Override
    public String createTemporaryReadUrlFromUrl(String url, long expiresInSeconds) {
        return url;
    }
}
