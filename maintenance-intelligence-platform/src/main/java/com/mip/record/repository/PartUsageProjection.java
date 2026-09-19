package com.mip.record.repository;

import java.time.LocalDate;

public interface PartUsageProjection {

    Long getPartId();

    long getUsageCount();

    LocalDate getLastUsedDate();
}
