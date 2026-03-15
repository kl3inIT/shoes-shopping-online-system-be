package com.sba.ssos.dto.response.dashboard;

import java.time.LocalDate;

public record DashboardChartPointResponse(
        LocalDate date,
        Double revenue,
        Double profit
) {
}

