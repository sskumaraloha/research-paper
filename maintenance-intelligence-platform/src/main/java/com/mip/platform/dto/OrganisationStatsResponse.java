package com.mip.platform.dto;

import java.time.Instant;

/** One subscriber organisation's usage at a glance. */
public record OrganisationStatsResponse(
        Long id,
        String code,
        String name,
        long plantCount,
        long userCount,
        long machineCount,
        long recordCount,
        long importJobCount,
        Instant lastActivityAt
) {
}
