package com.sba.ssos.dto.request.product.shoe;

public record ShoeStockRequest(
        long total,
        long selling,
        long outOfStock,
        long lowStock
) {}