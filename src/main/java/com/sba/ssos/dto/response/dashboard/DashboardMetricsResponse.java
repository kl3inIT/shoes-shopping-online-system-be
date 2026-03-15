package com.sba.ssos.dto.response.dashboard;

public record DashboardMetricsResponse(
        Double totalRevenue,
        Long totalCustomers,
        Long totalOrders,
        Long productsSold
) {
}

