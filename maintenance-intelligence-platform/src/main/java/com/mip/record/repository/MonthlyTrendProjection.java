package com.mip.record.repository;

public interface MonthlyTrendProjection {

    int getYear();

    int getMonth();

    long getRecordCount();

    long getTotalDowntimeMinutes();
}
