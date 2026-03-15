package com.sba.ssos.dto.response.dashboard;

import com.sba.ssos.enums.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record DashboardRecentOrderResponse(
        UUID id,
        String orderCode,
        String customerName,
        Instant createdAt,
        Double totalAmount,
        OrderStatus status
) {
}

