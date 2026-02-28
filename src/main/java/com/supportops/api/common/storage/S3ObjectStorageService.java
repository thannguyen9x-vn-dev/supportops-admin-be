package com.supportops.api.common.storage;

import com.supportops.api.config.AppStorageProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppStorageProperties storageProperties;

    @PostConstruct
    void ensureBucketExists() {
        String bucket = storageProperties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.storage.bucket is required");
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ex) {
            log.warn("Skipping storage bucket check due to connection/setup issue: {}", ex.getMessage());
        }
    }

    @Override
    public String uploadPublicObject(String key, byte[] data, String contentType) {
        String bucket = storageProperties.getBucket();

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(Objects.requireNonNullElse(contentType, "application/octet-stream"))
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
        return buildPublicUrl(key);
    }

    @Override
    public void deleteObjectByUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        String key = extractKey(url);
        if (key == null || key.isBlank()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(storageProperties.getBucket())
            .key(key)
            .build());
    }

    @Override
    public String createTemporaryReadUrlFromUrl(String url, long expiresInSeconds) {
        String key = extractKey(url);
        if (key == null || key.isBlank()) {
            return url;
        }

        long safeTtl = Math.max(60L, Math.min(3600L, expiresInSeconds));
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(storageProperties.getBucket())
            .key(key)
            .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(safeTtl))
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    private String buildPublicUrl(String key) {
        String endpoint = storageProperties.getEndpoint();
        String trimmedEndpoint = endpoint != null ? endpoint.replaceAll("/+$", "") : "";
        return trimmedEndpoint + "/" + storageProperties.getBucket() + "/" + key;
    }

    private String extractKey(String url) {
        String prefix = buildPublicUrl("");
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }

        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }

        return null;
    }
}
