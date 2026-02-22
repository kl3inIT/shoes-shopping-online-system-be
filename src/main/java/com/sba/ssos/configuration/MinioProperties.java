package com.sba.ssos.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint = "https://s3.nhuxuanviet.com";
    private String bucket = "ssos-images";
    private String accessKey;
    private String secretKey;
    private int presignedExpirySeconds = 3600;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public int getPresignedExpirySeconds() {
        return presignedExpirySeconds;
    }

    public void setPresignedExpirySeconds(int presignedExpirySeconds) {
        this.presignedExpirySeconds = presignedExpirySeconds;
    }
}
