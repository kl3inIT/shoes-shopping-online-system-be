package com.sba.ssos.dto.request.order;

import java.util.UUID;

public record OrderExpiredRequest(
        UUID orderId
) {
}
