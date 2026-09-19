package com.mip.record.repository;

public interface MachineDowntimeProjection {

    Long getMachineId();

    String getMachineCode();

    String getMachineName();

    long getRecordCount();

    long getTotalDowntimeMinutes();
}
