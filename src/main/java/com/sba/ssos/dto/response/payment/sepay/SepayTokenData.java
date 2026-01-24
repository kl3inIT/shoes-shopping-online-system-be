package com.sba.ssos.dto.response.payment.sepay;


public record SepayTokenData(
        String accessToken,
        String refreshToken,
        Long expiredIn
) {
}

