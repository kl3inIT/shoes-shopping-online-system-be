package com.sba.ssos.configuration;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(ApplicationProperties props) {
        var minio = props.minioProperties();

        if (minio == null
                || minio.accessKey() == null || minio.accessKey().isBlank()
                || minio.secretKey() == null || minio.secretKey().isBlank()) {
            throw new IllegalStateException(
                    "MinIO accessKey/secretKey must be configured and non-blank when using MinioClient."
            );
        }

        return MinioClient.builder()
                .endpoint(minio.endpoint())
                .credentials(minio.accessKey(), minio.secretKey())
                .build();
    }
}