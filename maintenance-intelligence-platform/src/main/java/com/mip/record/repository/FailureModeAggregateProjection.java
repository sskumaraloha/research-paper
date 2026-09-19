package com.mip.record.repository;

import com.mip.dictionary.entity.FailureCategory;

public interface FailureModeAggregateProjection {

    Long getFailureModeId();

    String getName();

    FailureCategory getCategory();

    long getRecordCount();

    long getTotalDowntimeMinutes();

    long getMachineCount();
}
