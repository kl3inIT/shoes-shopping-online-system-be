package com.sba.ssos.dto.request.order.sepay;

import jakarta.validation.constraints.NotBlank;

public record SepayTokenRequest(
    @NotBlank(message = "validation.sepay.client_id.required") String clientId,
    @NotBlank(message = "validation.sepay.client_secret.required") String clientSecret) {}
