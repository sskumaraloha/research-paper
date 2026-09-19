package com.mip.importjob.repository;

import com.mip.importjob.entity.ImportJobStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobStepRepository extends JpaRepository<ImportJobStep, Long> {

    List<ImportJobStep> findByJobIdOrderByOrderIndexAsc(Long jobId);
}
