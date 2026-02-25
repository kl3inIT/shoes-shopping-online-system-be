package com.sba.ssos.dto.response.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
    UUID id,
    UUID shoeId,
    UUID shoeVariantId,
    String shoeName,
    String shoeSlug,
    String shoeImage,
    String size,
    String color,
    Long quantity,
    BigDecimal price,
    BigDecimal subtotal,
    Long stock
) {}
