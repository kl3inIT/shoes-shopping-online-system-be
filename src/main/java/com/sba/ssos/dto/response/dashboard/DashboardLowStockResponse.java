package com.sba.ssos.dto.response.dashboard;

public record DashboardLowStockResponse(
        String productName,
        String size,
        Long remaining,
        String status
) {
}

