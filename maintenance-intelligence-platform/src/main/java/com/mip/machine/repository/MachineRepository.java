package com.mip.machine.repository;

import com.mip.machine.entity.Machine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    Optional<Machine> findByIdAndPlantId(Long id, Long plantId);

    Optional<Machine> findByPlantIdAndCodeIgnoreCase(Long plantId, String code);

    List<Machine> findByPlantIdAndActiveTrue(Long plantId);

    long countByPlantId(Long plantId);

    long countByPlantIdIn(List<Long> plantIds);

    @Query("""
            select m from Machine m
            where m.plant.id = :plantId
              and (:lineId is null or m.line.id = :lineId)
              and (:criticality is null or m.criticality = :criticality)
              and (:query is null
                   or lower(m.name) like lower(concat('%', :query, '%'))
                   or lower(m.code) like lower(concat('%', :query, '%')))
            """)
    Page<Machine> search(@Param("plantId") Long plantId,
                         @Param("lineId") Long lineId,
                         @Param("criticality") com.mip.machine.entity.Criticality criticality,
                         @Param("query") String query,
                         Pageable pageable);

    @Query("""
            select m from Machine m
            where m.plant.id in :plantIds
              and (lower(m.name) like lower(concat('%', :query, '%'))
                   or lower(m.code) like lower(concat('%', :query, '%')))
            """)
    List<Machine> searchAcrossPlants(@Param("plantIds") List<Long> plantIds,
                                     @Param("query") String query,
                                     Pageable pageable);
}
