package com.mip.record.repository;

import java.time.LocalDate;

public interface PartDateProjection {

    Long getPartId();

    String getPartName();

    LocalDate getRecordDate();

    Long getMachineId();
}
