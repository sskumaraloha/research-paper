package com.mip.schedule.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompleteScheduleRequest(
        /** Defaults to today. */
        @PastOrPresent LocalDate performedOn,
        /** Planned maintenance may still cost downtime; defaults to 0. */
        @PositiveOrZero Integer downtimeMinutes,
        @Size(max = 2000) String notes,
        @Size(max = 100) String technician
) {
}
