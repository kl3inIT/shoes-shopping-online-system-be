package com.sba.ssos.dto.response.payment.sepay;


public record SepayTokenResponse(SepayTokenData data) {
    public record SepayTokenData(
            String accessToken,
            Long expiredIn
    ) {
    }
}

