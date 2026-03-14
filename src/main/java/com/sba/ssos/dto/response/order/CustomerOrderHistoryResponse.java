package com.sba.ssos.dto.response.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Một đơn hàng trong danh sách đơn hàng theo customer.
 * Khớp cấu trúc FE: id, orderNumber, status, createdAt, items, total.
 */
public record CustomerOrderHistoryResponse(
        UUID id,
        String orderNumber,
        String status,
        Instant createdAt,
        List<CustomerOrderItemResponse> items,
        Double total
) {
}
