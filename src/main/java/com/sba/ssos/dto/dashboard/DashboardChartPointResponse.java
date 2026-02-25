package com.sba.ssos.dto.dashboard;

import java.time.LocalDate;

public record DashboardChartPointResponse(
        LocalDate date,
        Double revenue,
        Double profit
) {
}

