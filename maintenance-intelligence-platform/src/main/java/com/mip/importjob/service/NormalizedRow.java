package com.mip.importjob.service;

import java.time.LocalDate;
import java.util.List;

/** A raw import row mapped onto the canonical maintenance-record fields. */
public record NormalizedRow(
        String machineText,
        LocalDate date,
        Integer downtimeMinutes,
        String description,
        String actionTaken,
        String technician,
        String failureModeText,
        String partsText,
        List<String> missingFields
) {
}
