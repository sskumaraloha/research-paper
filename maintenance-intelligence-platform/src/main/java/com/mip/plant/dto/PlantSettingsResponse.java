package com.mip.plant.dto;

public record PlantSettingsResponse(
        Long plantId,
        String plantName,
        double autoApproveThreshold,
        double lowConfidenceThreshold,
        int downtimeAlertMinutes
) {
}
