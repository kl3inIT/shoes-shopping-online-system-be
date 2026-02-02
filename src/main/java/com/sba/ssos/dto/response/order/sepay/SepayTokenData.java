package com.sba.ssos.dto.response.order.sepay;


public record SepayTokenData(
        String accessToken,
        String refreshToken,
        Long expiredIn
) {
}

