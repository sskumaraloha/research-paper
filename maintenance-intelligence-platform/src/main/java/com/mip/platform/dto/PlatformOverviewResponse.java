package com.mip.platform.dto;

/** Global platform health for the software owner's console. */
public record PlatformOverviewResponse(
        long organisations,
        long plants,
        long users,
        long machines,
        long maintenanceRecords,
        long recordsLast30Days,
        long importJobs,
        long pendingValidations
) {
}
