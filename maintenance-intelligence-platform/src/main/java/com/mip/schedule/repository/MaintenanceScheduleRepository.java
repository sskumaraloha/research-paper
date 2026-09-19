package com.mip.schedule.repository;

import com.mip.schedule.entity.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {

    List<MaintenanceSchedule> findByPlantIdOrderByNextDueOnAsc(Long plantId);

    List<MaintenanceSchedule> findByPlantIdAndActiveTrueOrderByNextDueOnAsc(Long plantId);

    List<MaintenanceSchedule> findByPlantIdAndActiveTrueAndNextDueOnLessThanEqualOrderByNextDueOnAsc(
            Long plantId, LocalDate dueBy);

    /** Overdue schedules that have not been notified today, across all plants. */
    @Query("""
            select s from MaintenanceSchedule s
            where s.active = true and s.nextDueOn < :today
              and (s.lastOverdueNotifiedOn is null or s.lastOverdueNotifiedOn < :today)
            """)
    List<MaintenanceSchedule> findOverdueNeedingNotification(@Param("today") LocalDate today);
}
