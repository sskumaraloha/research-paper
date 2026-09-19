package com.mip.plant.repository;

import com.mip.plant.entity.ProductionLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, Long> {

    List<ProductionLine> findByPlantIdOrderByCodeAsc(Long plantId);

    Optional<ProductionLine> findByPlantIdAndCodeIgnoreCase(Long plantId, String code);
}
