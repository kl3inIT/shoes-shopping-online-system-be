package com.sba.ssos.dto.response.dashboard;

public record DashboardTopSellingResponse(
        String productName,
        String categoryName,
        Long totalSold,
        Long currentStock
) {
}

