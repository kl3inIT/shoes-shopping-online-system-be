package com.sba.ssos.dto.response.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreateResponse(
        UUID orderId,
        String orderCode,
        String bankNumber,
        String bankCode,
        String accountHolder,
        Double amount,
        String status,
        LocalDateTime expiredAt
) {
}
