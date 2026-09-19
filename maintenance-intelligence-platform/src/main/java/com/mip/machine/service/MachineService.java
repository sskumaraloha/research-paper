package com.mip.machine.service;

import com.mip.common.dto.PageResponse;
import com.mip.exception.InvalidRequestException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.machine.dto.AliasResponse;
import com.mip.machine.dto.MachineDetailResponse;
import com.mip.machine.dto.MachineRowResponse;
import com.mip.machine.entity.Criticality;
import com.mip.machine.entity.Machine;
import com.mip.machine.entity.MachineStatus;
import com.mip.machine.repository.MachineRepository;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordStatus;
import com.mip.record.repository.MachineActivityProjection;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineService {

    /** A recent record with downtime at/above the plant alert threshold marks a machine ATTENTION. */
    private static final int ATTENTION_WINDOW_DAYS = 14;

    private final MachineRepository machineRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final MachineAliasService machineAliasService;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public PageResponse<MachineRowResponse> listMachines(Long plantId, String query, Long lineId,
                                                         String criticality, int page, int size,
                                                         MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        Criticality criticalityFilter = parseCriticality(criticality);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name"));
        String textFilter = (query == null || query.isBlank()) ? null : query.trim();
        Page<Machine> machines = machineRepository.search(plantId, lineId, criticalityFilter,
                textFilter, pageable);

        Map<Long, MachineActivityProjection> activity = activityByMachine(
                machines.getContent().stream().map(Machine::getId).toList());

        return PageResponse.of(machines.map(machine -> toRow(machine, activity.get(machine.getId()))));
    }

    @Transactional(readOnly = true)
    public MachineDetailResponse getMachineDetail(Long machineId, MipUserDetails principal) {
        Machine machine = requireAccessibleMachine(machineId, principal);
        MachineActivityProjection activity = activityByMachine(List.of(machine.getId()))
                .get(machine.getId());
        List<AliasResponse> aliases = machineAliasService.listAliases(machine.getId());
        return new MachineDetailResponse(
                machine.getId(),
                machine.getPlant().getId(), machine.getPlant().getName(),
                machine.getLine() == null ? null : machine.getLine().getId(),
                machine.getLine() == null ? null : machine.getLine().getName(),
                machine.getCode(), machine.getName(),
                machine.getManufacturer(), machine.getModel(),
                machine.getCriticality().name(), machine.getCommissionedOn(), machine.isActive(),
                deriveStatus(machine).name(),
                activity == null ? 0 : activity.getRecordCount(),
                activity == null ? 0 : activity.getTotalDowntimeMinutes(),
                activity == null ? null : activity.getLastRecordDate(),
                aliases);
    }

    /**
     * Derived machine health: ATTENTION when the latest record is recent and reported
     * downtime at/above the plant alert threshold; UNKNOWN with no history; else RUNNING.
     */
    @Transactional(readOnly = true)
    public MachineStatus deriveStatus(Machine machine) {
        List<MaintenanceRecord> latest = recordRepository
                .findTop1ByMachineIdAndStatusOrderByRecordDateDescIdDesc(machine.getId(), RecordStatus.ACTIVE);
        if (latest.isEmpty()) {
            return MachineStatus.UNKNOWN;
        }
        MaintenanceRecord record = latest.get(0);
        boolean recent = !record.getRecordDate().isBefore(LocalDate.now().minusDays(ATTENTION_WINDOW_DAYS));
        boolean severe = record.getDowntimeMinutes() >= machine.getPlant().getDowntimeAlertMinutes();
        return (recent && severe) ? MachineStatus.ATTENTION : MachineStatus.RUNNING;
    }

    /** Loads a machine after verifying the principal can access its plant (404 otherwise). */
    @Transactional(readOnly = true)
    public Machine requireAccessibleMachine(Long machineId, MipUserDetails principal) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", machineId));
        plantService.requireAccessiblePlant(machine.getPlant().getId(), principal);
        return machine;
    }

    private MachineRowResponse toRow(Machine machine, MachineActivityProjection activity) {
        return new MachineRowResponse(
                machine.getId(), machine.getCode(), machine.getName(),
                machine.getLine() == null ? null : machine.getLine().getName(),
                machine.getCriticality().name(),
                deriveStatus(machine).name(),
                activity == null ? 0 : activity.getRecordCount(),
                activity == null ? 0 : activity.getTotalDowntimeMinutes(),
                activity == null ? null : activity.getLastRecordDate());
    }

    private Map<Long, MachineActivityProjection> activityByMachine(List<Long> machineIds) {
        if (machineIds.isEmpty()) {
            return Map.of();
        }
        return recordRepository.machineActivity(machineIds).stream()
                .collect(Collectors.toMap(MachineActivityProjection::getMachineId, Function.identity()));
    }

    private Criticality parseCriticality(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Criticality.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unknown criticality: " + value);
        }
    }
}
