package com.sba.ssos.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "validation.cart.quantity.required")
        @Min(value = 1, message = "validation.cart.quantity.min")
        Long quantity
) {}
