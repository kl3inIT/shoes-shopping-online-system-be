package com.sba.ssos.dto.request.order;

import com.sba.ssos.enums.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;

public record OrderHistoryRequest(
    @Min(value = 0, message = "validation.page.min") int page,
    @Min(value = 1, message = "validation.page.size.min")
        @Max(value = 100, message = "validation.page.size.max")
        int size,
    String nameSearch,
    Instant dateFrom,
    Instant dateTo,
    OrderStatus orderStatus) {}
