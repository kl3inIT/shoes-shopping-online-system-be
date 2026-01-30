package com.sba.ssos.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID shoeVariantId,
        @Min(1) long quantity
) {}
