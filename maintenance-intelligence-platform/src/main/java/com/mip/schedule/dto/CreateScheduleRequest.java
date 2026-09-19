package com.mip.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateScheduleRequest(
        @NotNull Long machineId,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @Min(1) @Max(3650) int intervalDays,
        /** First due date; defaults to today + intervalDays. A past date is immediately overdue. */
        LocalDate firstDueOn
) {
}
