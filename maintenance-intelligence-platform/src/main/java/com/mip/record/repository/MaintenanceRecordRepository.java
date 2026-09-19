package com.mip.record.repository;

import com.mip.dictionary.entity.FailureMode;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    Optional<MaintenanceRecord> findByIdAndPlantId(Long id, Long plantId);

    Page<MaintenanceRecord> findByMachineIdAndStatusOrderByRecordDateDescIdDesc(
            Long machineId, RecordStatus status, Pageable pageable);

    List<MaintenanceRecord> findByMachineIdAndStatusOrderByRecordDateDescIdDesc(
            Long machineId, RecordStatus status);

    long countByPlantIdAndStatus(Long plantId, RecordStatus status);

    @Query("""
            select r from MaintenanceRecord r
            where r.plant.id = :plantId
              and r.status = :status
              and (:machineId is null or r.machine.id = :machineId)
              and (:lineId is null or r.machine.line.id = :lineId)
              and (:failureModeId is null or r.failureMode.id = :failureModeId)
              and (:from is null or r.recordDate >= :from)
              and (:to is null or r.recordDate <= :to)
              and (:text is null
                   or lower(r.description) like lower(concat('%', :text, '%'))
                   or lower(r.actionTaken) like lower(concat('%', :text, '%'))
                   or lower(r.technician) like lower(concat('%', :text, '%')))
            """)
    Page<MaintenanceRecord> search(@Param("plantId") Long plantId,
                                   @Param("status") RecordStatus status,
                                   @Param("machineId") Long machineId,
                                   @Param("lineId") Long lineId,
                                   @Param("failureModeId") Long failureModeId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("text") String text,
                                   Pageable pageable);

    @Query("""
            select r from MaintenanceRecord r
            where r.plant.id in :plantIds and r.status = 'ACTIVE'
              and (lower(r.description) like lower(concat('%', :text, '%'))
                   or lower(r.actionTaken) like lower(concat('%', :text, '%')))
            order by r.recordDate desc
            """)
    List<MaintenanceRecord> fullTextAcrossPlants(@Param("plantIds") List<Long> plantIds,
                                                 @Param("text") String text,
                                                 Pageable pageable);

    @Query("select distinct r.failureMode from MaintenanceRecord r "
            + "where r.plant.id = :plantId and r.status = 'ACTIVE' and r.failureMode is not null")
    List<FailureMode> findDistinctFailureModesInUse(@Param("plantId") Long plantId);

    // --- aggregates ---

    @Query("""
            select r.machine.id as machineId, count(r) as recordCount,
                   coalesce(sum(r.downtimeMinutes), 0) as totalDowntimeMinutes,
                   max(r.recordDate) as lastRecordDate
            from MaintenanceRecord r
            where r.machine.id in :machineIds and r.status = 'ACTIVE'
            group by r.machine.id
            """)
    List<MachineActivityProjection> machineActivity(@Param("machineIds") List<Long> machineIds);

    @Query("""
            select r.failureMode.id as failureModeId, r.failureMode.name as name,
                   r.failureMode.category as category,
                   count(r) as recordCount, coalesce(sum(r.downtimeMinutes), 0) as totalDowntimeMinutes
            from MaintenanceRecord r
            where r.plant.id = :plantId and r.status = 'ACTIVE' and r.failureMode is not null
              and (:machineId is null or r.machine.id = :machineId)
              and (:from is null or r.recordDate >= :from)
              and (:to is null or r.recordDate <= :to)
            group by r.failureMode.id, r.failureMode.name, r.failureMode.category
            order by sum(r.downtimeMinutes) desc
            """)
    List<FailureModeAggregateProjection> failureModeAggregates(@Param("plantId") Long plantId,
                                                               @Param("machineId") Long machineId,
                                                               @Param("from") LocalDate from,
                                                               @Param("to") LocalDate to);

    @Query("""
            select r.machine.id as machineId, r.machine.code as machineCode, r.machine.name as machineName,
                   count(r) as recordCount, coalesce(sum(r.downtimeMinutes), 0) as totalDowntimeMinutes
            from MaintenanceRecord r
            where r.plant.id = :plantId and r.status = 'ACTIVE'
              and (:from is null or r.recordDate >= :from)
              and (:to is null or r.recordDate <= :to)
            group by r.machine.id, r.machine.code, r.machine.name
            order by sum(r.downtimeMinutes) desc
            """)
    List<MachineDowntimeProjection> machineDowntime(@Param("plantId") Long plantId,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);

    @Query("""
            select r.machine.line.id as lineId, r.machine.line.name as lineName,
                   count(r) as recordCount, coalesce(sum(r.downtimeMinutes), 0) as totalDowntimeMinutes
            from MaintenanceRecord r
            where r.plant.id = :plantId and r.status = 'ACTIVE' and r.machine.line is not null
              and (:from is null or r.recordDate >= :from)
              and (:to is null or r.recordDate <= :to)
            group by r.machine.line.id, r.machine.line.name
            order by sum(r.downtimeMinutes) desc
            """)
    List<LineDowntimeProjection> lineDowntime(@Param("plantId") Long plantId,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    @Query("""
            select year(r.recordDate) as year, month(r.recordDate) as month,
                   count(r) as recordCount, coalesce(sum(r.downtimeMinutes), 0) as totalDowntimeMinutes
            from MaintenanceRecord r
            where r.plant.id = :plantId and r.status = 'ACTIVE'
              and r.recordDate >= :from
            group by year(r.recordDate), month(r.recordDate)
            order by year(r.recordDate), month(r.recordDate)
            """)
    List<MonthlyTrendProjection> monthlyTrend(@Param("plantId") Long plantId,
                                              @Param("from") LocalDate from);

    @Query("""
            select coalesce(sum(r.downtimeMinutes), 0)
            from MaintenanceRecord r
            where r.plant.id = :plantId and r.status = 'ACTIVE'
              and r.recordDate >= :from and r.recordDate <= :to
            """)
    long totalDowntimeBetween(@Param("plantId") Long plantId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    long countByPlantIdAndStatusAndRecordDateBetween(Long plantId, RecordStatus status,
                                                     LocalDate from, LocalDate to);

    @Query("""
            select r from MaintenanceRecord r join r.spareParts p
            where p.id = :partId and r.status = 'ACTIVE' and r.plant.id in :plantIds
            order by r.recordDate asc
            """)
    List<MaintenanceRecord> findByPartUsage(@Param("partId") Long partId,
                                            @Param("plantIds") List<Long> plantIds);

    @Query("""
            select p.id as partId, count(r) as usageCount, max(r.recordDate) as lastUsedDate
            from MaintenanceRecord r join r.spareParts p
            where r.status = 'ACTIVE' and r.plant.id in :plantIds and p.id in :partIds
            group by p.id
            """)
    List<PartUsageProjection> partUsage(@Param("partIds") List<Long> partIds,
                                        @Param("plantIds") List<Long> plantIds);

    @Query("""
            select r from MaintenanceRecord r
            where r.machine.id = :machineId and r.status = 'ACTIVE'
              and r.failureMode.id = :failureModeId
            order by r.recordDate desc
            """)
    List<MaintenanceRecord> findByMachineAndFailureMode(@Param("machineId") Long machineId,
                                                        @Param("failureModeId") Long failureModeId);

    List<MaintenanceRecord> findTop1ByMachineIdAndStatusOrderByRecordDateDescIdDesc(
            Long machineId, RecordStatus status);
}
