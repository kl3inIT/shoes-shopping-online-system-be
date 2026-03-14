package com.sba.ssos.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sba.ssos.configuration.ApplicationProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private ApplicationProperties applicationProperties;

    @Test
    void getPresignedGetUrlReturnsNullWhenObjectKeyIsBlank() {
        MinioStorageService service = new MinioStorageService(minioClient, applicationProperties);

        String presignedUrl = service.getPresignedGetUrl("   ");

        assertThat(presignedUrl).isNull();
        verifyNoInteractions(minioClient, applicationProperties);
    }

    @Test
    void getPresignedPutUrlRejectsBlankObjectKey() {
        MinioStorageService service = new MinioStorageService(minioClient, applicationProperties);

        assertThatThrownBy(() -> service.getPresignedPutUrl(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Object key must not be blank to generate a presigned PUT URL.");

        verifyNoInteractions(minioClient, applicationProperties);
    }
}
