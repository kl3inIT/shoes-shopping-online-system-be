package com.sba.ssos.dto.response.upload;

import java.io.InputStream;

public record FileResource(
        InputStream inputStream,
        String contentType
) {
}

