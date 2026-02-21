package com.sba.ssos.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        UUID shoeVariantId,

        UUID shoeId,

        String size,

        String color,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Long quantity
) {}
