package com.sba.ssos.dto.request.order;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderExpiredRequest(@NotNull(message = "validation.order.id.required") UUID orderId) {}
