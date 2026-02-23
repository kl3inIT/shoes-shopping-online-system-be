package com.sba.ssos.dto.response.order;

import com.sba.ssos.enums.OrderStatus;

public record OrderPaid(
        String orderCode,
        OrderStatus status
) {}
