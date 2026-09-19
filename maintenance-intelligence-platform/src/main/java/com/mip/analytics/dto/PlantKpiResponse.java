package com.mip.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record PlantKpiResponse(
        Long plantId,
        LocalDate windowFrom,
        LocalDate windowTo,
        List<KpiValue> kpis
) {
}
