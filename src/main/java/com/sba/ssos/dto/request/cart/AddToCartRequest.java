package com.sba.ssos.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull(message = "validation.cart.shoe_variant_id.required")
        UUID shoeVariantId,

        @NotNull(message = "validation.cart.quantity.required")
        @Min(value = 1, message = "validation.cart.quantity.min")
        Long quantity
) {}
