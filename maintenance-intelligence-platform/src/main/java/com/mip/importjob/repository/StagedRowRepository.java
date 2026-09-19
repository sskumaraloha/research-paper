package com.mip.importjob.repository;

import com.mip.importjob.entity.StagedRow;
import com.mip.importjob.entity.StagedRowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StagedRowRepository extends JpaRepository<StagedRow, Long> {

    List<StagedRow> findByJobIdOrderByRowNumberAsc(Long jobId);

    long countByJobIdAndStatus(Long jobId, StagedRowStatus status);
}
