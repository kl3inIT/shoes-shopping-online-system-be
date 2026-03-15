package com.sba.ssos.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "validation.order.item.shoe_variant_id.required") UUID shoeVariantId,
        @Min(value = 1, message = "validation.order.item.quantity.min") long quantity
) {}
