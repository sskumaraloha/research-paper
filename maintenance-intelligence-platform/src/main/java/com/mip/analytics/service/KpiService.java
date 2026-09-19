package com.mip.analytics.service;

import com.mip.analytics.dto.KpiValue;
import com.mip.analytics.dto.PlantKpiResponse;
import com.mip.machine.repository.MachineRepository;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.RecordStatus;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Plant-level KPIs over a 30-day window, each compared against the previous 30 days. */
@Service
@RequiredArgsConstructor
public class KpiService {

    static final int WINDOW_DAYS = 30;

    private final MaintenanceRecordRepository recordRepository;
    private final MachineRepository machineRepository;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public PlantKpiResponse plantKpis(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        LocalDate today = LocalDate.now();
        LocalDate windowFrom = today.minusDays(WINDOW_DAYS - 1);
        LocalDate previousFrom = windowFrom.minusDays(WINDOW_DAYS);
        LocalDate previousTo = windowFrom.minusDays(1);

        long downtime = recordRepository.totalDowntimeBetween(plantId, windowFrom, today);
        long previousDowntime = recordRepository.totalDowntimeBetween(plantId, previousFrom, previousTo);
        long events = recordRepository.countByPlantIdAndStatusAndRecordDateBetween(plantId,
                RecordStatus.ACTIVE, windowFrom, today);
        long previousEvents = recordRepository.countByPlantIdAndStatusAndRecordDateBetween(plantId,
                RecordStatus.ACTIVE, previousFrom, previousTo);
        long activeMachines = machineRepository.findByPlantIdAndActiveTrue(plantId).size();

        double avgDowntime = events == 0 ? 0 : (double) downtime / events;
        double previousAvg = previousEvents == 0 ? 0 : (double) previousDowntime / previousEvents;

        List<KpiValue> kpis = List.of(
                kpi("downtimeMinutes", "Total downtime (min)", downtime, previousDowntime,
                        previousEvents > 0 || previousDowntime > 0),
                kpi("maintenanceEvents", "Maintenance events", events, previousEvents,
                        previousEvents > 0),
                kpi("avgDowntimePerEvent", "Avg downtime per event (min)", avgDowntime, previousAvg,
                        previousEvents > 0),
                new KpiValue("activeMachines", "Active machines", activeMachines, null, null));

        return new PlantKpiResponse(plantId, windowFrom, today, kpis);
    }

    private KpiValue kpi(String key, String label, double value, double previous, boolean hasPrevious) {
        Double changePct = null;
        if (hasPrevious && previous != 0) {
            changePct = Math.round(1000.0 * (value - previous) / previous) / 10.0;
        }
        return new KpiValue(key, label, Math.round(value * 10.0) / 10.0,
                hasPrevious ? Math.round(previous * 10.0) / 10.0 : null, changePct);
    }
}
