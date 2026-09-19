package com.mip.platform.dto;

import com.mip.user.dto.UserSummaryResponse;

import java.util.List;

public record OrganisationDetailResponse(
        Long id,
        String code,
        String name,
        List<PlantStats> plants,
        List<UserSummaryResponse> users
) {
    public record PlantStats(
            Long id,
            String code,
            String name,
            long machineCount,
            long recordCount,
            long downtimeLast30DaysMinutes,
            long pendingValidations
    ) {
    }
}
