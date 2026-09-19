package com.mip.assistant.service;

import com.mip.analytics.dto.FailureModeStatsResponse;
import com.mip.analytics.dto.KpiValue;
import com.mip.analytics.dto.MachineDowntimeResponse;
import com.mip.analytics.dto.MachineStatsResponse;
import com.mip.analytics.dto.PlantKpiResponse;
import com.mip.analytics.service.AnalyticsService;
import com.mip.analytics.service.KpiService;
import com.mip.assistant.dto.AnswerSection;
import com.mip.assistant.dto.AssistantAnswerResponse;
import com.mip.assistant.dto.StatTile;
import com.mip.common.entity.BaseEntity;
import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.part.entity.SparePart;
import com.mip.plant.service.PlantService;
import com.mip.record.dto.RecordRowResponse;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordStatus;
import com.mip.record.mapper.RecordMapper;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.search.service.QueryNormalizerService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns a routed intent into a structured, data-backed answer. */
@Service
@RequiredArgsConstructor
public class AnswerBuilderService {

    private static final int WINDOW_DAYS = 90;
    private static final int RECORD_LIMIT = 5;

    private final AnalyticsService analyticsService;
    private final KpiService kpiService;
    private final MaintenanceRecordRepository recordRepository;
    private final QueryNormalizerService queryNormalizerService;
    private final PlantService plantService;
    private final RecordMapper recordMapper;

    @Transactional(readOnly = true)
    public AssistantAnswerResponse machineHistory(Machine machine, MipUserDetails principal) {
        MachineStatsResponse stats = analyticsService.machineStats(machine.getId(), principal);
        List<RecordRowResponse> recent = recordRepository
                .findByMachineIdAndStatusOrderByRecordDateDescIdDesc(machine.getId(),
                        RecordStatus.ACTIVE, PageRequest.of(0, RECORD_LIMIT))
                .map(recordMapper::toRow).getContent();

        List<StatTile> tiles = List.of(
                new StatTile("Maintenance events", String.valueOf(stats.recordCount()), null),
                new StatTile("Total downtime", String.valueOf(stats.totalDowntimeMinutes()), "min"),
                new StatTile("MTBF", stats.mtbfDays() == null ? "n/a" : stats.mtbfDays().toString(), "days"),
                new StatTile("Status", stats.status(), null));

        List<AnswerSection> sections = new ArrayList<>();
        if (!stats.topFailureModes().isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (FailureModeStatsResponse mode : stats.topFailureModes()) {
                text.append("- ").append(mode.name()).append(": ").append(mode.recordCount())
                        .append(" events, ").append(mode.totalDowntimeMinutes()).append(" min downtime\n");
            }
            sections.add(new AnswerSection("Top failure modes", text.toString().stripTrailing()));
        }

        return new AssistantAnswerResponse(Intent.MACHINE_HISTORY.name(),
                "History of " + machine.getName() + " (" + machine.getCode() + ")",
                tiles, sections, recent,
                List.of("Why does " + machine.getName() + " keep failing?",
                        "Show downtime trend for the plant"));
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse repeatedFailures(Long plantId, Machine machine) {
        LocalDate from = LocalDate.now().minusDays(WINDOW_DAYS);
        List<FailureModeStatsResponse> repeated = recordRepository
                .failureModeAggregates(plantId, machine == null ? null : machine.getId(), from, null)
                .stream()
                .filter(m -> m.getRecordCount() >= (machine == null ? 3 : 2))
                .map(m -> new FailureModeStatsResponse(m.getFailureModeId(), m.getName(),
                        m.getCategory().name(), m.getRecordCount(), m.getTotalDowntimeMinutes(),
                        0, m.getMachineCount()))
                .toList();

        String scope = machine == null ? "the plant" : machine.getName();
        List<AnswerSection> sections = new ArrayList<>();
        if (repeated.isEmpty()) {
            sections.add(new AnswerSection("No repeat offenders",
                    "No failure mode recurred often enough on " + scope + " in the last "
                            + WINDOW_DAYS + " days to stand out."));
        } else {
            StringBuilder text = new StringBuilder();
            for (FailureModeStatsResponse mode : repeated) {
                text.append("- ").append(mode.name()).append(": ").append(mode.recordCount())
                        .append(" occurrences");
                if (machine == null) {
                    text.append(" across ").append(mode.machinesAffected()).append(" machines");
                }
                text.append(", ").append(mode.totalDowntimeMinutes()).append(" min downtime\n");
            }
            sections.add(new AnswerSection("Recurring failures (last " + WINDOW_DAYS + " days)",
                    text.toString().stripTrailing()));
        }
        return new AssistantAnswerResponse(Intent.REPEATED_FAILURES.name(),
                "Recurring failures on " + scope, List.of(), sections, List.of(),
                List.of("Which machine has the highest downtime?"));
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse failureModeDowntime(Long plantId, FailureMode failureMode) {
        LocalDate from = LocalDate.now().minusDays(WINDOW_DAYS);
        FailureModeStatsResponse stats = recordRepository
                .failureModeAggregates(plantId, null, from, null).stream()
                .filter(m -> m.getFailureModeId().equals(failureMode.getId()))
                .findFirst()
                .map(m -> new FailureModeStatsResponse(m.getFailureModeId(), m.getName(),
                        m.getCategory().name(), m.getRecordCount(), m.getTotalDowntimeMinutes(),
                        m.getRecordCount() == 0 ? 0
                                : (double) m.getTotalDowntimeMinutes() / m.getRecordCount(),
                        m.getMachineCount()))
                .orElse(new FailureModeStatsResponse(failureMode.getId(), failureMode.getName(),
                        failureMode.getCategory().name(), 0, 0, 0, 0));

        List<RecordRowResponse> recent = recordRepository.search(plantId, RecordStatus.ACTIVE, null,
                        null, failureMode.getId(), from, null, null,
                        PageRequest.of(0, RECORD_LIMIT, Sort.by(Sort.Direction.DESC, "recordDate", "id")))
                .map(recordMapper::toRow).getContent();

        List<StatTile> tiles = List.of(
                new StatTile("Events", String.valueOf(stats.recordCount()), null),
                new StatTile("Total downtime", String.valueOf(stats.totalDowntimeMinutes()), "min"),
                new StatTile("Machines affected", String.valueOf(stats.machinesAffected()), null),
                new StatTile("Avg per event", String.valueOf(Math.round(stats.avgDowntimeMinutes())), "min"));

        return new AssistantAnswerResponse(Intent.FAILURE_MODE_DOWNTIME.name(),
                "'" + failureMode.getName() + "' over the last " + WINDOW_DAYS + " days",
                tiles, List.of(), recent,
                List.of("Show the downtime pareto", "Which machine has the highest downtime?"));
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse partUsage(Long plantId, SparePart part) {
        List<MaintenanceRecord> usages = recordRepository.findByPartUsage(part.getId(), List.of(plantId));
        Map<String, Integer> byMachine = new LinkedHashMap<>();
        for (MaintenanceRecord record : usages) {
            byMachine.merge(record.getMachine().getName(), 1, Integer::sum);
        }
        Double avgInterval = null;
        if (usages.size() >= 2) {
            long span = ChronoUnit.DAYS.between(usages.get(0).getRecordDate(),
                    usages.get(usages.size() - 1).getRecordDate());
            avgInterval = Math.round(10.0 * span / (usages.size() - 1)) / 10.0;
        }
        List<StatTile> tiles = List.of(
                new StatTile("Times used", String.valueOf(usages.size()), null),
                new StatTile("Machines", String.valueOf(byMachine.size()), null),
                new StatTile("Avg replacement interval",
                        avgInterval == null ? "n/a" : avgInterval.toString(), "days"),
                new StatTile("Last used", usages.isEmpty() ? "never"
                        : usages.get(usages.size() - 1).getRecordDate().toString(), null));

        List<AnswerSection> sections = new ArrayList<>();
        if (!byMachine.isEmpty()) {
            StringBuilder text = new StringBuilder();
            byMachine.forEach((name, count) ->
                    text.append("- ").append(name).append(": ").append(count).append(" times\n"));
            sections.add(new AnswerSection("Usage by machine", text.toString().stripTrailing()));
        }
        List<RecordRowResponse> recent = usages.stream()
                .sorted(Comparator.comparing(MaintenanceRecord::getRecordDate).reversed())
                .limit(RECORD_LIMIT).map(recordMapper::toRow).toList();

        return new AssistantAnswerResponse(Intent.PART_USAGE.name(),
                "Usage of " + part.getName() + " (" + part.getPartNumber() + ")",
                tiles, sections, recent, List.of("Show part replacement intervals"));
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse highestDowntime(Long plantId, MipUserDetails principal) {
        LocalDate from = LocalDate.now().minusDays(WINDOW_DAYS);
        List<MachineDowntimeResponse> top =
                analyticsService.topMachinesByDowntime(plantId, from, null, 5, principal);
        List<StatTile> tiles = new ArrayList<>();
        List<AnswerSection> sections = new ArrayList<>();
        if (top.isEmpty()) {
            sections.add(new AnswerSection("No downtime recorded",
                    "No maintenance events were recorded in the last " + WINDOW_DAYS + " days."));
        } else {
            MachineDowntimeResponse worst = top.get(0);
            tiles.add(new StatTile("Worst machine", worst.machineName(), null));
            tiles.add(new StatTile("Its downtime", String.valueOf(worst.downtimeMinutes()), "min"));
            tiles.add(new StatTile("Its events", String.valueOf(worst.recordCount()), null));
            StringBuilder text = new StringBuilder();
            int rank = 1;
            for (MachineDowntimeResponse machine : top) {
                text.append(rank++).append(". ").append(machine.machineName())
                        .append(" (").append(machine.machineCode()).append("): ")
                        .append(machine.downtimeMinutes()).append(" min over ")
                        .append(machine.recordCount()).append(" events\n");
            }
            sections.add(new AnswerSection("Top machines by downtime (last " + WINDOW_DAYS + " days)",
                    text.toString().stripTrailing()));
        }
        return new AssistantAnswerResponse(Intent.HIGHEST_DOWNTIME.name(),
                "Highest-downtime machines", tiles, sections, List.of(),
                List.of("Why does the top machine keep failing?", "Show the downtime pareto"));
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse recordSearch(String question, MipUserDetails principal) {
        List<Long> plantIds = plantService.accessiblePlants(principal).stream()
                .map(BaseEntity::getId).toList();
        Map<Long, MaintenanceRecord> matches = new LinkedHashMap<>();
        for (String variant : queryNormalizerService.expandTerms(question)) {
            for (MaintenanceRecord record : recordRepository.fullTextAcrossPlants(plantIds, variant,
                    PageRequest.of(0, RECORD_LIMIT * 2))) {
                matches.putIfAbsent(record.getId(), record);
            }
        }
        List<RecordRowResponse> records = matches.values().stream()
                .sorted(Comparator.comparing(MaintenanceRecord::getRecordDate).reversed())
                .limit(RECORD_LIMIT).map(recordMapper::toRow).toList();
        List<AnswerSection> sections = records.isEmpty()
                ? List.of(new AnswerSection("No matches",
                        "No maintenance records matched. Try a machine name, failure mode or part."))
                : List.of(new AnswerSection("Matching records",
                        "Found " + matches.size() + " matching records; showing the most recent."));
        return new AssistantAnswerResponse(Intent.RECORD_SEARCH.name(),
                "Records matching \"" + question + "\"", List.of(), sections, records, List.of());
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse similarFailures(Long plantId, FailureMode failureMode,
                                                   String question, MipUserDetails principal) {
        if (failureMode == null) {
            return recordSearch(question, principal);
        }
        List<RecordRowResponse> similar = recordRepository.search(plantId, RecordStatus.ACTIVE, null,
                        null, failureMode.getId(), null, null, null,
                        PageRequest.of(0, RECORD_LIMIT, Sort.by(Sort.Direction.DESC, "recordDate", "id")))
                .map(recordMapper::toRow).getContent();
        List<AnswerSection> sections = List.of(new AnswerSection("Similar past failures",
                similar.isEmpty()
                        ? "No earlier '" + failureMode.getName() + "' events are on record for this plant."
                        : "Most recent '" + failureMode.getName() + "' events in this plant, with the "
                        + "actions that resolved them."));
        return new AssistantAnswerResponse(Intent.SIMILAR_FAILURES.name(),
                "Similar failures: " + failureMode.getName(), List.of(), sections, similar,
                List.of("Total downtime caused by " + failureMode.getName()));
    }

    @Transactional(readOnly = true)
    public AssistantAnswerResponse plantKpis(Long plantId, MipUserDetails principal) {
        PlantKpiResponse kpis = kpiService.plantKpis(plantId, principal);
        List<StatTile> tiles = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (KpiValue kpi : kpis.kpis()) {
            tiles.add(new StatTile(kpi.label(), trimNumber(kpi.value()), null));
            if (kpi.changePct() != null) {
                text.append("- ").append(kpi.label()).append(": ")
                        .append(kpi.changePct() >= 0 ? "up " : "down ")
                        .append(Math.abs(kpi.changePct())).append("% vs the previous 30 days\n");
            }
        }
        List<AnswerSection> sections = text.isEmpty() ? List.of()
                : List.of(new AnswerSection("vs previous window", text.toString().stripTrailing()));
        return new AssistantAnswerResponse(Intent.PLANT_KPIS.name(),
                "Plant KPIs (" + kpis.windowFrom() + " to " + kpis.windowTo() + ")",
                tiles, sections, List.of(),
                List.of("Which machine has the highest downtime?", "Show recurring failures"));
    }

    public AssistantAnswerResponse fallback(List<String> suggestions) {
        return new AssistantAnswerResponse(Intent.FALLBACK.name(),
                "I didn't catch that",
                List.of(),
                List.of(new AnswerSection("What I can answer",
                        "Ask about a machine's history, recurring failures, downtime by failure "
                                + "mode, spare part usage, the worst machines, or plant KPIs.")),
                List.of(), suggestions);
    }

    private String trimNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
