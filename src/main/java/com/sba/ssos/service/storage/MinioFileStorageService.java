package com.sba.ssos.service.storage;

import com.sba.ssos.configuration.MinioProperties;
import com.sba.ssos.dto.response.upload.FileResource;
import com.sba.ssos.exception.base.InternalServerErrorException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("shoes", "shoevariants");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

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
        String bucketName = minioProperties.getBucket();

        ensureBucketExists(bucketName);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
        } catch (Exception e) {
            throw new InternalServerErrorException("error.storage.minio", e);
        }

        return objectKey;
    }

    public FileResource getFile(String objectKey) {
        String bucketName = minioProperties.getBucket();
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            String contentType = guessContentType(objectKey);
            return new FileResource(stream, contentType);
        } catch (Exception e) {
            throw new InternalServerErrorException("error.storage.minio", e);
        }
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.trim().isEmpty()) {
            return "";
        }
        String trimmed = folder.trim().toLowerCase();

        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new InternalServerErrorException("error.storage.minio", new IllegalArgumentException("Invalid folder"));
        }

        if (!ALLOWED_FOLDERS.contains(trimmed)) {
            throw new InternalServerErrorException("error.storage.minio", new IllegalArgumentException("Folder not allowed"));
        }
        return trimmed;
    }

    //phan chinh sua path
    private void ensureBucketExists(String bucketName) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
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

