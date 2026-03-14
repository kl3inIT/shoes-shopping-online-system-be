package com.sba.ssos.dto.response.order;

import java.util.UUID;

/**
 * Item trong đơn hàng (dòng sản phẩm) cho API danh sách đơn hàng theo customer.
 */
public record CustomerOrderItemResponse(
        UUID id,
        String name,
        String image,
        Double price,
        String size,
        Long quantity
) {
}
