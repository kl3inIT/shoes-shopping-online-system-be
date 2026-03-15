package com.sba.ssos.controller.dashboard;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.dashboard.DashboardChartPointResponse;
import com.sba.ssos.dto.response.dashboard.DashboardLowStockResponse;
import com.sba.ssos.dto.response.dashboard.DashboardMetricsResponse;
import com.sba.ssos.dto.response.dashboard.DashboardRecentOrderResponse;
import com.sba.ssos.dto.response.dashboard.DashboardTopSellingResponse;
import com.sba.ssos.service.DashboardService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ADMIN_DASHBOARD)
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Dashboard", description = "Administrative dashboard metrics endpoints")
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
            @RequestParam(name = "days", defaultValue = "90")
            @Min(value = 1, message = "validation.dashboard.days.min") int days
    ) {
        List<DashboardChartPointResponse> data = dashboardService.getChartData(days);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.chart"), data);
    }

    @GetMapping("/recent-orders")
    public ResponseGeneral<List<DashboardRecentOrderResponse>> getRecentOrders(
            @RequestParam(name = "limit", defaultValue = "10")
            @Min(value = 1, message = "validation.dashboard.limit.min") int limit
    ) {
        List<DashboardRecentOrderResponse> data = dashboardService.getRecentOrders(limit);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.recentOrders"), data);
    }

    @GetMapping("/top-selling")
    public ResponseGeneral<List<DashboardTopSellingResponse>> getTopSelling(
            @RequestParam(name = "limit", defaultValue = "10")
            @Min(value = 1, message = "validation.dashboard.limit.min") int limit
    ) {
        List<DashboardTopSellingResponse> data = dashboardService.getTopSelling(limit);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.topSelling"), data);
    }

    @GetMapping("/low-stock")
    public ResponseGeneral<List<DashboardLowStockResponse>> getLowStock(
            @RequestParam(name = "threshold", defaultValue = "5")
            @Min(value = 0, message = "validation.dashboard.threshold.min") int threshold
    ) {
        List<DashboardLowStockResponse> data = dashboardService.getLowStock(threshold);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.dashboard.lowStock"), data);
    }
}

