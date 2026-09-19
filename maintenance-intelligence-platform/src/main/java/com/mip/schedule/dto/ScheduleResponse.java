package com.mip.schedule.dto;

import java.time.LocalDate;

public record ScheduleResponse(
        Long id,
        Long machineId,
        String machineName,
        String title,
        String description,
        int intervalDays,
        LocalDate lastPerformedOn,
        LocalDate nextDueOn,
        long daysUntilDue,
        /** OVERDUE, DUE_SOON (within 7 days) or SCHEDULED. */
        String status,
        boolean active
) {
}
