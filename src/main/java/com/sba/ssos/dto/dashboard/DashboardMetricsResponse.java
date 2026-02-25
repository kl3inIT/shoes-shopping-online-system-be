package com.sba.ssos.dto.dashboard;

public record DashboardMetricsResponse(
        Double totalRevenue,
        Long totalCustomers,
        Long totalOrders,
        Long productsSold
) {
}

