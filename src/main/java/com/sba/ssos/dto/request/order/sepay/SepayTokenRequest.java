package com.sba.ssos.dto.request.order.sepay;

public record SepayTokenRequest(
        String clientId,
        String clientSecret
) {
}
