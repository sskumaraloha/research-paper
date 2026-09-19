package com.mip.importjob.repository;

import com.mip.importjob.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    Optional<ImportJob> findByIdAndPlantId(Long id, Long plantId);

    Optional<ImportJob> findTopByPlantIdOrderByCreatedAtDesc(Long plantId);

    long countByPlantIdIn(List<Long> plantIds);
}
