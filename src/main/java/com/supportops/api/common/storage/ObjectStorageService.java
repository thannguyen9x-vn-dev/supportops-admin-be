package com.supportops.api.common.storage;

public interface ObjectStorageService {

    String uploadPublicObject(String key, byte[] data, String contentType);

    void deleteObjectByUrl(String url);

    String createTemporaryReadUrlFromUrl(String url, long expiresInSeconds);
}
