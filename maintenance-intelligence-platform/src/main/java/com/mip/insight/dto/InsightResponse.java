package com.mip.insight.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InsightResponse(
        Long id,
        String type,
        String severity,
        String title,
        String detail,
        Long machineId,
        String machineName,
        Double metricValue,
        int windowDays,
        Instant computedAt,
        List<Evidence> evidence
) {
    public record Evidence(Long recordId, LocalDate recordDate, String description, String note) {
    }
}
