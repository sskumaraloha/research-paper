package com.mip.record.repository;

public interface LineDowntimeProjection {

    Long getLineId();

    String getLineName();

    long getRecordCount();

    long getTotalDowntimeMinutes();
}
