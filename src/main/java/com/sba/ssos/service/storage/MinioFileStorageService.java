package com.sba.ssos.service.storage;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.response.upload.FileResource;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.InternalServerErrorException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("shoes", "shoevariants", "reviews", "avatars");

    private final MinioClient minioClient;
    private final ApplicationProperties applicationProperties;

    public String upload(MultipartFile file) {
        return upload(file, "");
    }

    public String upload(MultipartFile file, String folder) {
        String safeFolder = normalizeFolder(folder);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String fileName = UUID.randomUUID() + fileExtension;
        String objectKey = safeFolder.isEmpty() ? fileName : safeFolder + "/" + fileName;
        String bucketName = applicationProperties.minioProperties().bucket();

        ensureBucketExists(bucketName);

        try {
            log.info("Uploading file to storage folder {}", safeFolder.isEmpty() ? "<root>" : safeFolder);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to upload file to storage", e);
            throw new InternalServerErrorException("error.storage.minio", e);
        }

        return objectKey;
    }

    public FileResource getFile(String objectKey) {
        String bucketName = applicationProperties.minioProperties().bucket();
        try {
            log.debug("Fetching file from storage");
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            String contentType = guessContentType(objectKey);
            return new FileResource(stream, contentType);
        } catch (Exception e) {
            log.error("Failed to fetch file from storage", e);
            throw new InternalServerErrorException("error.storage.minio", e);
        }
    }

    public void delete(String objectKey) {
        String bucketName = applicationProperties.minioProperties().bucket();
        try {
            log.info("Deleting file from storage");
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete file from storage", e);
            throw new InternalServerErrorException("error.storage.minio", e);
        }
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.trim().isEmpty()) {
            return "";
        }
        String trimmed = folder.trim().toLowerCase();

        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new BadRequestException("error.storage.folder.invalid");
        }

        if (!ALLOWED_FOLDERS.contains(trimmed)) {
            throw new BadRequestException("error.storage.folder.not_allowed");
        }
        return trimmed;
    }

    private void ensureBucketExists(String bucketName) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                log.info("Creating storage bucket {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            log.error("Failed to ensure storage bucket exists", e);
            throw new InternalServerErrorException("error.storage.minio", e);
        }
    }

    private String guessContentType(String objectKey) {
        String lower = objectKey.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}

