package com.sba.ssos.dto.request.product.shoe;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ShoeVariantImageUpdateRequest(
        @NotNull
        UUID variantId,
        List<String> keepImageUrls
) {
}
