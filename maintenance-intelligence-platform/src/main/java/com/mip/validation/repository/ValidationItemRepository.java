package com.mip.validation.repository;

import com.mip.validation.entity.ValidationItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ValidationItemRepository extends JpaRepository<ValidationItem, Long> {

    Optional<ValidationItem> findByIdAndPlantId(Long id, Long plantId);

    Page<ValidationItem> findByPlantIdAndStatusOrderByCreatedAtAsc(
            Long plantId, ValidationItem.ValidationStatus status, Pageable pageable);

    long countByPlantIdAndStatus(Long plantId, ValidationItem.ValidationStatus status);

    @Query("""
            select v from ValidationItem v
            where v.plant.id = :plantId and v.status = 'PENDING'
              and v.stagedRow.machine is null and v.stagedRow.machineText is not null
            """)
    List<ValidationItem> findPendingWithUnresolvedMachine(@Param("plantId") Long plantId);
}
