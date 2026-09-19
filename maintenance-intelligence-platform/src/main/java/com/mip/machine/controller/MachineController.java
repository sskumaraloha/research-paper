package com.mip.machine.controller;

import com.mip.analytics.dto.MachineStatsResponse;
import com.mip.analytics.service.AnalyticsService;
import com.mip.common.dto.PageResponse;
import com.mip.insight.dto.InsightResponse;
import com.mip.insight.service.InsightService;
import com.mip.machine.dto.AddAliasRequest;
import com.mip.machine.dto.AliasResponse;
import com.mip.machine.dto.CreateMachineRequest;
import com.mip.machine.dto.MachineDetailResponse;
import com.mip.machine.dto.MachineRowResponse;
import com.mip.machine.dto.UpdateMachineRequest;
import com.mip.machine.entity.Machine;
import com.mip.machine.service.MachineAliasService;
import com.mip.machine.service.MachineService;
import com.mip.machine.entity.AliasSource;
import com.mip.record.dto.RecordRowResponse;
import com.mip.record.service.MaintenanceRecordService;
import com.mip.security.MipUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;
    private final MachineAliasService machineAliasService;
    private final MaintenanceRecordService recordService;
    private final AnalyticsService analyticsService;
    private final InsightService insightService;

    @GetMapping
    public PageResponse<MachineRowResponse> listMachines(
            @RequestParam Long plantId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) String criticality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MipUserDetails principal) {
        return machineService.listMachines(plantId, query, lineId, criticality, page, size, principal);
    }

    @GetMapping("/{machineId}")
    public MachineDetailResponse getMachine(@PathVariable Long machineId,
                                            @AuthenticationPrincipal MipUserDetails principal) {
        return machineService.getMachineDetail(machineId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public MachineDetailResponse createMachine(@Valid @RequestBody CreateMachineRequest request,
                                               @AuthenticationPrincipal MipUserDetails principal) {
        return machineService.createMachine(request, principal);
    }

    @PutMapping("/{machineId}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public MachineDetailResponse updateMachine(@PathVariable Long machineId,
                                               @Valid @RequestBody UpdateMachineRequest request,
                                               @AuthenticationPrincipal MipUserDetails principal) {
        return machineService.updateMachine(machineId, request, principal);
    }

    @GetMapping("/{machineId}/timeline")
    public PageResponse<RecordRowResponse> getTimeline(@PathVariable Long machineId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.getMachineTimeline(machineId, page, size, principal);
    }

    @GetMapping("/{machineId}/stats")
    public MachineStatsResponse getStats(@PathVariable Long machineId,
                                         @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.machineStats(machineId, principal);
    }

    @GetMapping("/{machineId}/insights")
    public List<InsightResponse> getInsights(@PathVariable Long machineId,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return insightService.getMachineInsights(machineId, principal);
    }

    @PostMapping("/{machineId}/insights/recompute")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public List<InsightResponse> recomputeInsights(@PathVariable Long machineId,
                                                   @AuthenticationPrincipal MipUserDetails principal) {
        return insightService.recomputeForMachine(machineId, principal);
    }

    @GetMapping("/{machineId}/aliases")
    public List<AliasResponse> listAliases(@PathVariable Long machineId,
                                           @AuthenticationPrincipal MipUserDetails principal) {
        machineService.requireAccessibleMachine(machineId, principal);
        return machineAliasService.listAliases(machineId);
    }

    @PostMapping("/{machineId}/aliases")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public AliasResponse addAlias(@PathVariable Long machineId,
                                  @Valid @RequestBody AddAliasRequest request,
                                  @AuthenticationPrincipal MipUserDetails principal) {
        Machine machine = machineService.requireAccessibleMachine(machineId, principal);
        return machineAliasService.addAlias(machine, request.alias(), AliasSource.MANUAL);
    }
}
