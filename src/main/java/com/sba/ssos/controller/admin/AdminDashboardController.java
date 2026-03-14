package com.sba.ssos.controller.admin;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.dashboard.DashboardChartPointResponse;
import com.sba.ssos.dto.dashboard.DashboardLowStockResponse;
import com.sba.ssos.dto.dashboard.DashboardMetricsResponse;
import com.sba.ssos.dto.dashboard.DashboardRecentOrderResponse;
import com.sba.ssos.dto.dashboard.DashboardTopSellingResponse;
import com.sba.ssos.service.DashboardService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final LocaleUtils localeUtils;

    @GetMapping("/metrics")
    public ResponseGeneral<DashboardMetricsResponse> getMetrics() {
        DashboardMetricsResponse data = dashboardService.getMetrics();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.metrics"), data);
    }

    @GetMapping("/chart")
    public ResponseGeneral<List<DashboardChartPointResponse>> getChart(
            @RequestParam(name = "days", defaultValue = "90") int days
    ) {
        List<DashboardChartPointResponse> data = dashboardService.getChartData(days);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.chart"), data);
    }

    @GetMapping("/recent-orders")
    public ResponseGeneral<List<DashboardRecentOrderResponse>> getRecentOrders(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<DashboardRecentOrderResponse> data = dashboardService.getRecentOrders(limit);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.recentOrders"), data);
    }

    @GetMapping("/top-selling")
    public ResponseGeneral<List<DashboardTopSellingResponse>> getTopSelling(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<DashboardTopSellingResponse> data = dashboardService.getTopSelling(limit);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.topSelling"), data);
    }

    @GetMapping("/low-stock")
    public ResponseGeneral<List<DashboardLowStockResponse>> getLowStock(
            @RequestParam(name = "threshold", defaultValue = "5") int threshold
    ) {
        List<DashboardLowStockResponse> data = dashboardService.getLowStock(threshold);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.lowStock"), data);
    }
}

