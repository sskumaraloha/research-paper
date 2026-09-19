package com.mip.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Field overrides applied to a staged row before approving it. All fields optional. */
public record EditValidationItemRequest(
        Long machineId,
        Long failureModeId,
        @PastOrPresent LocalDate recordDate,
        @PositiveOrZero Integer downtimeMinutes,
        @Size(max = 2000) String description,
        @Size(max = 2000) String actionTaken,
        @Size(max = 100) String technician,
        List<@NotBlank @Size(max = 150) String> partNames
) {
}
