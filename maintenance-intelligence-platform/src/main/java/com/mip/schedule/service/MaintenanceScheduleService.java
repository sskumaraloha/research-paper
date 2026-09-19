package com.mip.schedule.service;

import com.mip.audit.service.AuditService;
import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.MachineRepository;
import com.mip.notification.service.NotificationService;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.service.MaintenanceRecordService;
import com.mip.schedule.dto.CompleteScheduleRequest;
import com.mip.schedule.dto.CreateScheduleRequest;
import com.mip.schedule.dto.ScheduleResponse;
import com.mip.schedule.dto.UpdateScheduleRequest;
import com.mip.schedule.entity.MaintenanceSchedule;
import com.mip.schedule.repository.MaintenanceScheduleRepository;
import com.mip.security.MipUserDetails;
import com.mip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduleService {

    /** Schedules due within this many days show as DUE_SOON. */
    static final int DUE_SOON_DAYS = 7;

    private final MaintenanceScheduleRepository scheduleRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceRecordService recordService;
    private final NotificationService notificationService;
    private final PlantService plantService;
    private final UserService userService;
    private final AuditService auditService;

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request, MipUserDetails principal) {
        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine", request.machineId()));
        plantService.requireAccessiblePlant(machine.getPlant().getId(), principal);
        LocalDate firstDue = request.firstDueOn() != null ? request.firstDueOn()
                : LocalDate.now().plusDays(request.intervalDays());
        MaintenanceSchedule schedule = scheduleRepository.save(new MaintenanceSchedule(
                machine.getPlant(), machine, request.title().trim(),
                request.description() == null ? null : request.description().trim(),
                request.intervalDays(), firstDue));
        auditService.log(principal, "SCHEDULE_CREATED", "SCHEDULE", schedule.getId(),
                machine.getPlant().getId(), schedule.getTitle() + " on " + machine.getCode());
        log.info("Schedule {} created for machine {}", schedule.getId(), machine.getCode());
        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, UpdateScheduleRequest request,
                                           MipUserDetails principal) {
        MaintenanceSchedule schedule = requireAccessibleSchedule(scheduleId, principal);
        if (request.title() != null && !request.title().isBlank()) {
            schedule.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            schedule.setDescription(request.description().isBlank() ? null
                    : request.description().trim());
        }
        if (request.intervalDays() != null) {
            schedule.setIntervalDays(request.intervalDays());
        }
        if (request.nextDueOn() != null) {
            schedule.setNextDueOn(request.nextDueOn());
            schedule.setLastOverdueNotifiedOn(null);
        }
        if (request.active() != null) {
            schedule.setActive(request.active());
        }
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedules(Long plantId, boolean includeInactive,
                                                MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        List<MaintenanceSchedule> schedules = includeInactive
                ? scheduleRepository.findByPlantIdOrderByNextDueOnAsc(plantId)
                : scheduleRepository.findByPlantIdAndActiveTrueOrderByNextDueOnAsc(plantId);
        return schedules.stream().map(this::toResponse).toList();
    }

    /** Everything overdue or due within the next {@value DUE_SOON_DAYS} days. */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> listDue(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return scheduleRepository
                .findByPlantIdAndActiveTrueAndNextDueOnLessThanEqualOrderByNextDueOnAsc(
                        plantId, LocalDate.now().plusDays(DUE_SOON_DAYS))
                .stream().map(this::toResponse).toList();
    }

    /**
     * Marks the plan done: writes a PREVENTIVE maintenance record and rolls the next
     * due date to performedOn + interval.
     */
    @Transactional
    public ScheduleResponse completeSchedule(Long scheduleId, CompleteScheduleRequest request,
                                             MipUserDetails principal) {
        MaintenanceSchedule schedule = requireAccessibleSchedule(scheduleId, principal);
        if (!schedule.isActive()) {
            throw new BusinessRuleViolationException("An inactive schedule cannot be completed");
        }
        LocalDate performedOn = request.performedOn() != null ? request.performedOn() : LocalDate.now();
        String description = "Preventive maintenance: " + schedule.getTitle()
                + (request.notes() == null || request.notes().isBlank() ? ""
                        : " - " + request.notes().trim());
        MaintenanceRecord record = recordService.createPreventiveRecord(schedule.getMachine(),
                performedOn, request.downtimeMinutes() == null ? 0 : request.downtimeMinutes(),
                description, schedule.getDescription(), request.technician(),
                userService.getUser(principal.getId()));

        schedule.setLastPerformedOn(performedOn);
        schedule.setNextDueOn(performedOn.plusDays(schedule.getIntervalDays()));
        schedule.setLastOverdueNotifiedOn(null);
        auditService.log(principal, "SCHEDULE_COMPLETED", "SCHEDULE", schedule.getId(),
                schedule.getPlant().getId(), "record " + record.getId());
        log.info("Schedule {} completed; record {}; next due {}", scheduleId, record.getId(),
                schedule.getNextDueOn());
        return toResponse(schedule);
    }

    /**
     * Daily sweep: notify plant staff about overdue schedules, at most once per day
     * per schedule. Returns how many schedules were notified.
     */
    @Transactional
    public int notifyOverdueSchedules() {
        LocalDate today = LocalDate.now();
        List<MaintenanceSchedule> overdue = scheduleRepository.findOverdueNeedingNotification(today);
        for (MaintenanceSchedule schedule : overdue) {
            notificationService.onMaintenanceOverdue(schedule);
            schedule.setLastOverdueNotifiedOn(today);
        }
        if (!overdue.isEmpty()) {
            log.info("Overdue-maintenance sweep notified {} schedules", overdue.size());
        }
        return overdue.size();
    }

    private MaintenanceSchedule requireAccessibleSchedule(Long scheduleId, MipUserDetails principal) {
        MaintenanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", scheduleId));
        plantService.requireAccessiblePlant(schedule.getPlant().getId(), principal);
        return schedule;
    }

    private ScheduleResponse toResponse(MaintenanceSchedule schedule) {
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), schedule.getNextDueOn());
        String status = daysUntilDue < 0 ? "OVERDUE"
                : daysUntilDue <= DUE_SOON_DAYS ? "DUE_SOON" : "SCHEDULED";
        return new ScheduleResponse(schedule.getId(), schedule.getMachine().getId(),
                schedule.getMachine().getName(), schedule.getTitle(), schedule.getDescription(),
                schedule.getIntervalDays(), schedule.getLastPerformedOn(), schedule.getNextDueOn(),
                daysUntilDue, status, schedule.isActive());
    }
}
