package com.mip.analytics.controller;

import com.mip.analytics.dto.FailureModeStatsResponse;
import com.mip.analytics.dto.LineDowntimeShareResponse;
import com.mip.analytics.dto.MachineDowntimeResponse;
import com.mip.analytics.dto.ParetoBucketResponse;
import com.mip.analytics.dto.PartIntervalResponse;
import com.mip.analytics.dto.TrendResponse;
import com.mip.analytics.service.AnalyticsService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/pareto")
    public List<ParetoBucketResponse> pareto(
            @RequestParam Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.pareto(plantId, from, to, principal);
    }

    @GetMapping("/top-downtime-machines")
    public List<MachineDowntimeResponse> topMachinesByDowntime(
            @RequestParam Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.topMachinesByDowntime(plantId, from, to, limit, principal);
    }

    @GetMapping("/failure-mode-stats")
    public List<FailureModeStatsResponse> failureModeStats(
            @RequestParam Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.failureModeStats(plantId, from, to, principal);
    }

    @GetMapping("/line-downtime-share")
    public List<LineDowntimeShareResponse> lineDowntimeShare(
            @RequestParam Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.lineDowntimeShare(plantId, from, to, principal);
    }

    @GetMapping("/part-replacement-intervals")
    public List<PartIntervalResponse> partReplacementIntervals(
            @RequestParam Long plantId,
            @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.partReplacementIntervals(plantId, principal);
    }

    @GetMapping("/downtime-trend")
    public TrendResponse downtimeTrend(@RequestParam Long plantId,
                                       @RequestParam(defaultValue = "6") int months,
                                       @AuthenticationPrincipal MipUserDetails principal) {
        return analyticsService.downtimeTrend(plantId, months, principal);
    }
}
