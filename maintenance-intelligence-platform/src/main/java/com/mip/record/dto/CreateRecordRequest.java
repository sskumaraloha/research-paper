package com.mip.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateRecordRequest(
        @NotNull Long plantId,
        @NotNull Long machineId,
        @NotNull @PastOrPresent LocalDate recordDate,
        @PositiveOrZero int downtimeMinutes,
        @NotBlank @Size(max = 2000) String description,
        @Size(max = 2000) String actionTaken,
        @Size(max = 100) String technician,
        Long failureModeId,
        /** Free text used to resolve the failure mode when no id is supplied. */
        @Size(max = 200) String failureModeText,
        /** Part names or part numbers; unknown parts are created. */
        List<@NotBlank @Size(max = 150) String> partNames
) {
}
