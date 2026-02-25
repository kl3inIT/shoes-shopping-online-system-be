package com.sba.ssos.dto.dashboard;

public record DashboardTopSellingResponse(
        String productName,
        String categoryName,
        Long totalSold,
        Long currentStock
) {
}

