package com.mip.plant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePlantSettingsRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double autoApproveThreshold,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double lowConfidenceThreshold,
        @NotNull @Positive Integer downtimeAlertMinutes
) {
}
