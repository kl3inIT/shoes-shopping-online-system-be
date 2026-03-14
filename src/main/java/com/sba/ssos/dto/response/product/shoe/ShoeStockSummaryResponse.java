package com.sba.ssos.dto.response.product.shoe;

public record ShoeStockSummaryResponse(
        long total,
        long selling,
        long outOfStock,
        long lowStock
) {}
