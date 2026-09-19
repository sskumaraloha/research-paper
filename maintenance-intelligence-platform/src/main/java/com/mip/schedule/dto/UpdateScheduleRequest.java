package com.mip.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** All fields optional; only supplied fields change. */
public record UpdateScheduleRequest(
        @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @Min(1) @Max(3650) Integer intervalDays,
        LocalDate nextDueOn,
        Boolean active
) {
}
