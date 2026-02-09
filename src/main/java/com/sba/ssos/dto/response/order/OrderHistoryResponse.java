package com.sba.ssos.dto.response.order;

import com.sba.ssos.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID orderId,
        String orderCode,
        Instant orderDate,
        String orderStatus,
        PaymentStatus paymentStatus,
        String paymentMethod,
        int itemCount,
        Double totalAmount
) {
}

