package com.sba.ssos.dto.request.order;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty
        List<OrderItemRequest> items,
        Long discountId,
        @NotBlank
        String shippingName,
        @NotBlank
        String shippingPhone,
        @NotBlank
        String shippingAddress,

        String notes
) {}


