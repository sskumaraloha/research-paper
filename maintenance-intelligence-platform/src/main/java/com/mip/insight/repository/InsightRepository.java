package com.mip.insight.repository;

import com.mip.insight.entity.Insight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsightRepository extends JpaRepository<Insight, Long> {

    List<Insight> findByPlantIdOrderBySeverityDescComputedAtDesc(Long plantId);

    List<Insight> findByMachineIdOrderBySeverityDescComputedAtDesc(Long machineId);

    long countByPlantId(Long plantId);

    List<Insight> findByMachineId(Long machineId);

    List<Insight> findByPlantIdAndMachineIsNull(Long plantId);
}
