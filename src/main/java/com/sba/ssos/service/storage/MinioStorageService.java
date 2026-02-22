package com.sba.ssos.service.storage;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.sba.ssos.configuration.MinioProperties;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties props;

    public MinioStorageService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    /**
     * Presigned URL để xem/GET ảnh (frontend dùng làm src của img).
     */
    public String getPresignedGetUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .expiry(props.getPresignedExpirySeconds(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned GET URL for " + objectKey, e);
        }
    }

    /**
     * Presigned URL để upload (PUT). Frontend dùng PUT với body là file lên URL này.
     */
    public String getPresignedPutUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned PUT URL for " + objectKey, e);
        }
    }

    /**
     * Xóa object trong bucket.
     */
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object " + objectKey, e);
        }
    }
}
