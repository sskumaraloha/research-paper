package com.mip.analytics.service;

import com.mip.analytics.dto.FailureModeStatsResponse;
import com.mip.analytics.dto.LineDowntimeShareResponse;
import com.mip.analytics.dto.MachineDowntimeResponse;
import com.mip.analytics.dto.MachineStatsResponse;
import com.mip.analytics.dto.ParetoBucketResponse;
import com.mip.analytics.dto.PartIntervalResponse;
import com.mip.analytics.dto.TrendResponse;
import com.mip.machine.entity.Machine;
import com.mip.machine.service.MachineService;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordStatus;
import com.mip.record.repository.FailureModeAggregateProjection;
import com.mip.record.repository.LineDowntimeProjection;
import com.mip.record.repository.MachineDowntimeProjection;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.record.repository.MonthlyTrendProjection;
import com.mip.record.repository.PartDateProjection;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MaintenanceRecordRepository recordRepository;
    private final MachineService machineService;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public MachineStatsResponse machineStats(Long machineId, MipUserDetails principal) {
        Machine machine = machineService.requireAccessibleMachine(machineId, principal);
        List<MaintenanceRecord> records = recordRepository
                .findByMachineIdAndStatusOrderByRecordDateDescIdDesc(machineId, RecordStatus.ACTIVE);

        long totalDowntime = records.stream().mapToLong(MaintenanceRecord::getDowntimeMinutes).sum();
        Double mtbfDays = meanDaysBetween(records);
        List<FailureModeStatsResponse> topModes = recordRepository
                .failureModeAggregates(machine.getPlant().getId(), machineId, null, null).stream()
                .limit(5)
                .map(this::toFailureModeStats)
                .toList();

        return new MachineStatsResponse(machine.getId(), machine.getCode(), machine.getName(),
                machineService.deriveStatus(machine).name(),
                records.size(), totalDowntime,
                records.isEmpty() ? 0 : round1((double) totalDowntime / records.size()),
                mtbfDays,
                records.isEmpty() ? null : records.get(0).getRecordDate(),
                topModes);
    }

    @Transactional(readOnly = true)
    public List<ParetoBucketResponse> pareto(Long plantId, LocalDate from, LocalDate to,
                                             MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        List<FailureModeAggregateProjection> aggregates =
                recordRepository.failureModeAggregates(plantId, null, from, to);
        long grandTotal = aggregates.stream()
                .mapToLong(FailureModeAggregateProjection::getTotalDowntimeMinutes).sum();

        List<ParetoBucketResponse> buckets = new ArrayList<>();
        double cumulative = 0;
        for (FailureModeAggregateProjection aggregate : aggregates) {
            double share = grandTotal == 0 ? 0
                    : 100.0 * aggregate.getTotalDowntimeMinutes() / grandTotal;
            cumulative += share;
            buckets.add(new ParetoBucketResponse(aggregate.getFailureModeId(), aggregate.getName(),
                    aggregate.getCategory().name(), aggregate.getRecordCount(),
                    aggregate.getTotalDowntimeMinutes(), round1(share), round1(cumulative)));
        }
        return buckets;
    }

    @Transactional(readOnly = true)
    public List<MachineDowntimeResponse> topMachinesByDowntime(Long plantId, LocalDate from,
                                                               LocalDate to, int limit,
                                                               MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return recordRepository.machineDowntime(plantId, from, to).stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(m -> new MachineDowntimeResponse(m.getMachineId(), m.getMachineCode(),
                        m.getMachineName(), m.getRecordCount(), m.getTotalDowntimeMinutes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FailureModeStatsResponse> failureModeStats(Long plantId, LocalDate from, LocalDate to,
                                                           MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return recordRepository.failureModeAggregates(plantId, null, from, to).stream()
                .map(this::toFailureModeStats)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LineDowntimeShareResponse> lineDowntimeShare(Long plantId, LocalDate from, LocalDate to,
                                                             MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        List<LineDowntimeProjection> lines = recordRepository.lineDowntime(plantId, from, to);
        long total = lines.stream().mapToLong(LineDowntimeProjection::getTotalDowntimeMinutes).sum();
        return lines.stream()
                .map(l -> new LineDowntimeShareResponse(l.getLineId(), l.getLineName(),
                        l.getRecordCount(), l.getTotalDowntimeMinutes(),
                        total == 0 ? 0 : round1(100.0 * l.getTotalDowntimeMinutes() / total)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartIntervalResponse> partReplacementIntervals(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        Map<Long, List<PartDateProjection>> byPart = new LinkedHashMap<>();
        for (PartDateProjection usage : recordRepository.partUsageDates(plantId, null)) {
            byPart.computeIfAbsent(usage.getPartId(), k -> new ArrayList<>()).add(usage);
        }
        List<PartIntervalResponse> result = new ArrayList<>();
        for (List<PartDateProjection> usages : byPart.values()) {
            PartDateProjection first = usages.get(0);
            result.add(new PartIntervalResponse(first.getPartId(), first.getPartName(),
                    usages.size(), averageIntervalDays(usages)));
        }
        result.sort(Comparator.comparingLong(PartIntervalResponse::usageCount).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public TrendResponse downtimeTrend(Long plantId, int months, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        int window = Math.max(1, Math.min(months, 24));
        LocalDate from = LocalDate.now().minusMonths(window).withDayOfMonth(1);
        Map<String, TrendResponse.TrendPoint> byMonth = new LinkedHashMap<>();
        // pre-fill so months without records still chart as zero
        for (int i = 0; i <= window; i++) {
            LocalDate month = from.plusMonths(i);
            String key = "%d-%02d".formatted(month.getYear(), month.getMonthValue());
            byMonth.put(key, new TrendResponse.TrendPoint(key, 0, 0));
        }
        for (MonthlyTrendProjection point : recordRepository.monthlyTrend(plantId, from)) {
            String key = "%d-%02d".formatted(point.getYear(), point.getMonth());
            byMonth.put(key, new TrendResponse.TrendPoint(key, point.getRecordCount(),
                    point.getTotalDowntimeMinutes()));
        }
        return new TrendResponse(plantId, List.copyOf(byMonth.values()));
    }

    private FailureModeStatsResponse toFailureModeStats(FailureModeAggregateProjection aggregate) {
        return new FailureModeStatsResponse(aggregate.getFailureModeId(), aggregate.getName(),
                aggregate.getCategory().name(), aggregate.getRecordCount(),
                aggregate.getTotalDowntimeMinutes(),
                aggregate.getRecordCount() == 0 ? 0
                        : round1((double) aggregate.getTotalDowntimeMinutes() / aggregate.getRecordCount()),
                aggregate.getMachineCount());
    }

    /** Records arrive newest-first; MTBF is the mean gap between consecutive failures. */
    static Double meanDaysBetween(List<MaintenanceRecord> recordsNewestFirst) {
        if (recordsNewestFirst.size() < 2) {
            return null;
        }
        LocalDate newest = recordsNewestFirst.get(0).getRecordDate();
        LocalDate oldest = recordsNewestFirst.get(recordsNewestFirst.size() - 1).getRecordDate();
        long span = ChronoUnit.DAYS.between(oldest, newest);
        return round1((double) span / (recordsNewestFirst.size() - 1));
    }

    private static Double averageIntervalDays(List<PartDateProjection> usagesOldestFirst) {
        if (usagesOldestFirst.size() < 2) {
            return null;
        }
        long span = ChronoUnit.DAYS.between(usagesOldestFirst.get(0).getRecordDate(),
                usagesOldestFirst.get(usagesOldestFirst.size() - 1).getRecordDate());
        return round1((double) span / (usagesOldestFirst.size() - 1));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
