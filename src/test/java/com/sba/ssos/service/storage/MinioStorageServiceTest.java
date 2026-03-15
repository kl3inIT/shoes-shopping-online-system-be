package com.sba.ssos.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.exception.base.BadRequestException;
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
            .isInstanceOf(BadRequestException.class)
            .hasMessage("error.storage.object_key.required");

        verifyNoInteractions(minioClient, applicationProperties);
    }
}
