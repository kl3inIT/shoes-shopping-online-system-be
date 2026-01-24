package com.sba.ssos.dto.request.payment.sepay;

public record SepayTokenRequest(
        String clientId,
        String clientSecret
) {
}
