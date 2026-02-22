package com.sba.ssos.dto.response.order;

import com.sba.ssos.enums.PaymentMethod;
import com.sba.ssos.enums.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID orderId,
        String orderCode,
        Instant orderDate,
        String customerName,
        String customerEmail,
        String orderStatus,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        Long itemCount,
        Double totalAmount
) {
}

