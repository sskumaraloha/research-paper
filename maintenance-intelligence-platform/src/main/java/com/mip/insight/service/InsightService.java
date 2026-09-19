package com.mip.insight.service;

import com.mip.insight.dto.InsightResponse;
import com.mip.insight.entity.Insight;
import com.mip.insight.repository.InsightRepository;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.MachineRepository;
import com.mip.machine.service.MachineService;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final InsightRepository insightRepository;
    private final MachineRepository machineRepository;
    private final PatternDetectionService detectionService;
    private final PlantService plantService;
    private final MachineService machineService;

    @Transactional(readOnly = true)
    public List<InsightResponse> listInsights(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return insightRepository.findByPlantIdOrderBySeverityDescComputedAtDesc(plantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InsightResponse> getMachineInsights(Long machineId, MipUserDetails principal) {
        machineService.requireAccessibleMachine(machineId, principal);
        return insightRepository.findByMachineIdOrderBySeverityDescComputedAtDesc(machineId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long insightCount(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return insightRepository.countByPlantId(plantId);
    }

    /** Replaces the machine-scoped insights of one machine with a fresh detection run. */
    @Transactional
    public List<InsightResponse> recomputeForMachine(Long machineId, MipUserDetails principal) {
        Machine machine = machineService.requireAccessibleMachine(machineId, principal);
        insightRepository.deleteAll(insightRepository.findByMachineId(machineId));
        List<Insight> fresh = detectForMachine(machine);
        insightRepository.saveAll(fresh);
        log.info("Recomputed insights for machine {}: {} found", machine.getCode(), fresh.size());
        return fresh.stream().map(this::toResponse).toList();
    }

    /** Replaces every insight of the plant with a full detection run. */
    @Transactional
    public List<InsightResponse> recomputeAll(Long plantId, MipUserDetails principal) {
        Plant plant = plantService.requireAccessiblePlant(plantId, principal);
        insightRepository.deleteAll(insightRepository.findByPlantIdOrderBySeverityDescComputedAtDesc(plantId));

        List<Insight> fresh = new ArrayList<>();
        for (Machine machine : machineRepository.findByPlantIdAndActiveTrue(plantId)) {
            fresh.addAll(detectForMachine(machine));
        }
        detectionService.detectChronicTopMachine(plant).ifPresent(fresh::add);
        detectionService.detectDominantFailureMode(plant).ifPresent(fresh::add);

        insightRepository.saveAll(fresh);
        log.info("Recomputed insights for plant {}: {} found", plant.getCode(), fresh.size());
        return fresh.stream().map(this::toResponse).toList();
    }

    private List<Insight> detectForMachine(Machine machine) {
        List<Insight> insights = new ArrayList<>(detectionService.detectRepeatedFailures(machine));
        detectionService.detectRisingDowntime(machine).ifPresent(insights::add);
        insights.addAll(detectionService.detectPartWear(machine));
        return insights;
    }

    private InsightResponse toResponse(Insight insight) {
        List<InsightResponse.Evidence> evidence = insight.getEvidence().stream()
                .map(e -> new InsightResponse.Evidence(e.getRecord().getId(),
                        e.getRecord().getRecordDate(), e.getRecord().getDescription(), e.getNote()))
                .toList();
        return new InsightResponse(insight.getId(), insight.getType().name(),
                insight.getSeverity().name(), insight.getTitle(), insight.getDetail(),
                insight.getMachine() == null ? null : insight.getMachine().getId(),
                insight.getMachine() == null ? null : insight.getMachine().getName(),
                insight.getMetricValue(), insight.getWindowDays(), insight.getComputedAt(), evidence);
    }
}
