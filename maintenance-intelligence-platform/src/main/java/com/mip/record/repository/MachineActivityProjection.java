package com.mip.record.repository;

import java.time.LocalDate;

public interface MachineActivityProjection {

    Long getMachineId();

    long getRecordCount();

    long getTotalDowntimeMinutes();

    LocalDate getLastRecordDate();
}
