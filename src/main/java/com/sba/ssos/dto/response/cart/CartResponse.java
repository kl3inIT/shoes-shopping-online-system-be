package com.sba.ssos.dto.response.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    List<CartItemResponse> items,
    BigDecimal totalPrice,
    Integer totalQuantity
) {}
