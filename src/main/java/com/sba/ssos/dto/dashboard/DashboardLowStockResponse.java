package com.sba.ssos.dto.dashboard;

public record DashboardLowStockResponse(
        String productName,
        String size,
        Long remaining,
        String status
) {
}

