package com.sba.ssos.service.storage;

import com.sba.ssos.configuration.MinioProperties;
import com.sba.ssos.exception.base.InternalServerErrorException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String objectKey = UUID.randomUUID() + fileExtension;
        String bucketName = minioProperties.bucket();

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

        String endpoint = minioProperties.endpoint().replaceFirst("/$", "");
        return endpoint + "/" + bucketName + "/" + objectKey;
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
}

