package com.sba.ssos.dto.request.order;

import com.sba.ssos.enums.OrderStatus;

import java.time.Instant;

public record OrderHistoryRequest(
        int page,
        int size,
        String nameSearch,          // search theo shippingName hoặc customer name
        Instant dateFrom,           // filter CREATED_AT >= dateFrom
        Instant dateTo,             // filter CREATED_AT <= dateTo
        OrderStatus orderStatus
) {
}
