package com.sba.ssos.service.storage;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.InternalServerErrorException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
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
            log.debug("Generating presigned GET URL");
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(ap.minioProperties().bucket())
                    .object(normalizedObjectKey)
                    .expiry(ap.minioProperties().presignedExpirySeconds(), TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL", e);
            throw new InternalServerErrorException("error.storage.minio", e);
        }
    }

    public String getPresignedPutUrl(String objectKey) {
        String normalizedObjectKey = requireObjectKey(objectKey, "generate a presigned PUT URL");

        try {
            log.debug("Generating presigned PUT URL");
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(ap.minioProperties().bucket())
                    .object(normalizedObjectKey)
                    .expiry(ap.minioProperties().presignedExpirySeconds(), TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL", e);
            throw new InternalServerErrorException("error.storage.minio", e);
        }
    }

    public void deleteObject(String objectKey) {
        String normalizedObjectKey = requireObjectKey(objectKey, "delete an object");

        try {
            log.info("Deleting object from storage");
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(ap.minioProperties().bucket())
                    .object(normalizedObjectKey)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete object from storage", e);
            throw new InternalServerErrorException("error.storage.minio", e);
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
            throw new BadRequestException("error.storage.object_key.required", "action", action);
        }
        return normalizedObjectKey;
    }
}
