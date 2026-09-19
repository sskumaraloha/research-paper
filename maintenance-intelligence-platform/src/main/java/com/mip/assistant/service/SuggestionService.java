package com.mip.assistant.service;

import com.mip.plant.service.PlantService;
import com.mip.record.repository.MachineDowntimeProjection;
import com.mip.record.repository.FailureModeAggregateProjection;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data-aware question suggestions shown in the assistant UI. */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final MaintenanceRecordRepository recordRepository;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public List<String> suggestions(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        List<String> suggestions = new ArrayList<>();
        LocalDate from = LocalDate.now().minusDays(90);

        List<MachineDowntimeProjection> topMachines = recordRepository.machineDowntime(plantId, from, null);
        if (!topMachines.isEmpty()) {
            suggestions.add("Show the history of " + topMachines.get(0).getMachineName());
            suggestions.add("Why does " + topMachines.get(0).getMachineName() + " keep failing?");
        }
        List<FailureModeAggregateProjection> topModes =
                recordRepository.failureModeAggregates(plantId, null, from, null);
        if (!topModes.isEmpty()) {
            suggestions.add("How much downtime did " + topModes.get(0).getName() + " cause?");
        }
        suggestions.add("Which machine has the highest downtime?");
        suggestions.add("Show plant KPIs");
        return suggestions.stream().limit(5).toList();
    }
}
