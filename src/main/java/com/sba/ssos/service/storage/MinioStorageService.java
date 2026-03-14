package com.sba.ssos.service.storage;

import com.sba.ssos.configuration.ApplicationProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final ApplicationProperties ap;

    public MinioStorageService(MinioClient minioClient, ApplicationProperties ap) {
        this.minioClient = minioClient;
        this.ap = ap;
    }

    public String getPresignedGetUrl(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        if (normalizedObjectKey == null) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(ap.minioProperties().bucket())
                    .object(normalizedObjectKey)
                    .expiry(ap.minioProperties().presignedExpirySeconds(), TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned GET URL for " + normalizedObjectKey, e);
        }
    }

    public String getPresignedPutUrl(String objectKey) {
        String normalizedObjectKey = requireObjectKey(objectKey, "generate a presigned PUT URL");

        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(ap.minioProperties().bucket())
                    .object(normalizedObjectKey)
                    .expiry(ap.minioProperties().presignedExpirySeconds(), TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned PUT URL for " + normalizedObjectKey, e);
        }
    }

    public void deleteObject(String objectKey) {
        String normalizedObjectKey = requireObjectKey(objectKey, "delete an object");

        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(ap.minioProperties().bucket())
                    .object(normalizedObjectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object " + normalizedObjectKey, e);
        }
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return objectKey.trim();
    }

    private String requireObjectKey(String objectKey, String action) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        if (normalizedObjectKey == null) {
            throw new IllegalArgumentException("Object key must not be blank to " + action + ".");
        }
        return normalizedObjectKey;
    }
}
