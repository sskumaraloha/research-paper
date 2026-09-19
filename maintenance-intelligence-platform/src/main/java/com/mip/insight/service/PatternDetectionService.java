package com.mip.insight.service;

import com.mip.dictionary.entity.FailureMode;
import com.mip.insight.entity.Insight;
import com.mip.insight.entity.InsightEvidence;
import com.mip.insight.entity.InsightType;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordStatus;
import com.mip.record.repository.FailureModeAggregateProjection;
import com.mip.record.repository.MachineDowntimeProjection;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.record.repository.PartDateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The five pattern detectors. Each returns fully-built (unsaved) insights with
 * evidence attached; {@link InsightService} owns persistence and recompute scope.
 */
@Service
@RequiredArgsConstructor
public class PatternDetectionService {

    static final int REPEAT_WINDOW_DAYS = 90;
    static final int REPEAT_THRESHOLD = 3;
    static final int RISING_WINDOW_DAYS = 30;
    static final double RISING_RATIO = 1.5;
    static final int PART_WEAR_WINDOW_DAYS = 180;
    static final int PART_WEAR_THRESHOLD = 3;
    static final double CHRONIC_MACHINE_SHARE = 30.0;
    static final double DOMINANT_MODE_SHARE = 40.0;
    static final int PLANT_WINDOW_DAYS = 90;

    private final MaintenanceRecordRepository recordRepository;

    // --- machine-scoped detectors ---

    public List<Insight> detectRepeatedFailures(Machine machine) {
        LocalDate from = LocalDate.now().minusDays(REPEAT_WINDOW_DAYS);
        List<MaintenanceRecord> records = recordRepository
                .findByMachineIdAndStatusAndRecordDateGreaterThanEqual(machine.getId(),
                        RecordStatus.ACTIVE, from);
        Map<FailureMode, List<MaintenanceRecord>> byMode = new LinkedHashMap<>();
        for (MaintenanceRecord record : records) {
            if (record.getFailureMode() != null) {
                byMode.computeIfAbsent(record.getFailureMode(), k -> new ArrayList<>()).add(record);
            }
        }
        List<Insight> insights = new ArrayList<>();
        for (Map.Entry<FailureMode, List<MaintenanceRecord>> entry : byMode.entrySet()) {
            int occurrences = entry.getValue().size();
            if (occurrences < REPEAT_THRESHOLD) {
                continue;
            }
            Insight insight = new Insight(machine.getPlant(), machine, InsightType.REPEATED_FAILURES,
                    occurrences >= 5 ? Insight.Severity.CRITICAL : Insight.Severity.WARNING,
                    entry.getKey().getName() + " recurring on " + machine.getName(),
                    "'" + entry.getKey().getName() + "' occurred " + occurrences + " times on "
                            + machine.getName() + " in the last " + REPEAT_WINDOW_DAYS
                            + " days. A recurring failure usually has an unaddressed root cause.",
                    (double) occurrences, REPEAT_WINDOW_DAYS);
            entry.getValue().stream().limit(10).forEach(record ->
                    insight.getEvidence().add(new InsightEvidence(insight, record, null)));
            insights.add(insight);
        }
        return insights;
    }

    public Optional<Insight> detectRisingDowntime(Machine machine) {
        LocalDate today = LocalDate.now();
        LocalDate windowFrom = today.minusDays(RISING_WINDOW_DAYS - 1);
        LocalDate previousFrom = windowFrom.minusDays(RISING_WINDOW_DAYS);
        List<MaintenanceRecord> records = recordRepository
                .findByMachineIdAndStatusAndRecordDateGreaterThanEqual(machine.getId(),
                        RecordStatus.ACTIVE, previousFrom);

        long current = 0;
        long previous = 0;
        List<MaintenanceRecord> currentRecords = new ArrayList<>();
        for (MaintenanceRecord record : records) {
            if (!record.getRecordDate().isBefore(windowFrom)) {
                current += record.getDowntimeMinutes();
                currentRecords.add(record);
            } else {
                previous += record.getDowntimeMinutes();
            }
        }
        if (previous == 0 || current < 120 || (double) current / previous < RISING_RATIO) {
            return Optional.empty();
        }
        double ratio = Math.round(10.0 * current / previous) / 10.0;
        Insight insight = new Insight(machine.getPlant(), machine, InsightType.RISING_DOWNTIME,
                ratio >= 3 ? Insight.Severity.CRITICAL : Insight.Severity.WARNING,
                "Downtime rising on " + machine.getName(),
                machine.getName() + " lost " + current + " minutes in the last " + RISING_WINDOW_DAYS
                        + " days versus " + previous + " in the " + RISING_WINDOW_DAYS
                        + " days before (" + ratio + "x). The machine is degrading.",
                ratio, RISING_WINDOW_DAYS * 2);
        currentRecords.stream().limit(10).forEach(record ->
                insight.getEvidence().add(new InsightEvidence(insight, record, null)));
        return Optional.of(insight);
    }

    public List<Insight> detectPartWear(Machine machine) {
        LocalDate from = LocalDate.now().minusDays(PART_WEAR_WINDOW_DAYS);
        Map<Long, List<PartDateProjection>> byPart = new LinkedHashMap<>();
        for (PartDateProjection usage : recordRepository
                .partUsageDates(machine.getPlant().getId(), machine.getId())) {
            if (!usage.getRecordDate().isBefore(from)) {
                byPart.computeIfAbsent(usage.getPartId(), k -> new ArrayList<>()).add(usage);
            }
        }
        List<Insight> insights = new ArrayList<>();
        for (List<PartDateProjection> usages : byPart.values()) {
            if (usages.size() < PART_WEAR_THRESHOLD) {
                continue;
            }
            String partName = usages.get(0).getPartName();
            insights.add(new Insight(machine.getPlant(), machine, InsightType.PART_WEAR,
                    usages.size() >= 5 ? Insight.Severity.WARNING : Insight.Severity.INFO,
                    partName + " wearing out fast on " + machine.getName(),
                    "'" + partName + "' was replaced " + usages.size() + " times on "
                            + machine.getName() + " within " + PART_WEAR_WINDOW_DAYS
                            + " days. Check for a misalignment, overload or quality issue "
                            + "consuming this part.",
                    (double) usages.size(), PART_WEAR_WINDOW_DAYS));
        }
        return insights;
    }

    // --- plant-scoped detectors ---

    public Optional<Insight> detectChronicTopMachine(Plant plant) {
        LocalDate from = LocalDate.now().minusDays(PLANT_WINDOW_DAYS);
        List<MachineDowntimeProjection> machines =
                recordRepository.machineDowntime(plant.getId(), from, null);
        if (machines.size() < 2) {
            return Optional.empty();
        }
        long total = machines.stream().mapToLong(MachineDowntimeProjection::getTotalDowntimeMinutes).sum();
        MachineDowntimeProjection top = machines.get(0);
        if (total == 0) {
            return Optional.empty();
        }
        double share = Math.round(1000.0 * top.getTotalDowntimeMinutes() / total) / 10.0;
        if (share < CHRONIC_MACHINE_SHARE) {
            return Optional.empty();
        }
        return Optional.of(new Insight(plant, null, InsightType.CHRONIC_TOP_MACHINE,
                share >= 50 ? Insight.Severity.CRITICAL : Insight.Severity.WARNING,
                top.getMachineName() + " dominates plant downtime",
                top.getMachineName() + " (" + top.getMachineCode() + ") accounts for " + share
                        + "% of all downtime at " + plant.getName() + " in the last "
                        + PLANT_WINDOW_DAYS + " days (" + top.getTotalDowntimeMinutes()
                        + " minutes). It deserves a focused reliability review.",
                share, PLANT_WINDOW_DAYS));
    }

    public Optional<Insight> detectDominantFailureMode(Plant plant) {
        LocalDate from = LocalDate.now().minusDays(PLANT_WINDOW_DAYS);
        List<FailureModeAggregateProjection> modes =
                recordRepository.failureModeAggregates(plant.getId(), null, from, null);
        if (modes.size() < 2) {
            return Optional.empty();
        }
        long total = modes.stream()
                .mapToLong(FailureModeAggregateProjection::getTotalDowntimeMinutes).sum();
        FailureModeAggregateProjection top = modes.get(0);
        if (total == 0) {
            return Optional.empty();
        }
        double share = Math.round(1000.0 * top.getTotalDowntimeMinutes() / total) / 10.0;
        if (share < DOMINANT_MODE_SHARE) {
            return Optional.empty();
        }
        return Optional.of(new Insight(plant, null, InsightType.DOMINANT_FAILURE_MODE,
                Insight.Severity.WARNING,
                "'" + top.getName() + "' dominates failures plant-wide",
                "'" + top.getName() + "' causes " + share + "% of downtime at " + plant.getName()
                        + " across " + top.getMachineCount() + " machines in the last "
                        + PLANT_WINDOW_DAYS + " days. A systemic cause (spares quality, "
                        + "lubrication schedule, operating conditions) is likely.",
                share, PLANT_WINDOW_DAYS));
    }
}
