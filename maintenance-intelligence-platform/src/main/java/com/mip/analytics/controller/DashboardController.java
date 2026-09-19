package com.mip.analytics.controller;

import com.mip.analytics.dto.PlantKpiResponse;
import com.mip.analytics.service.KpiService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final KpiService kpiService;

    @GetMapping("/kpis")
    public PlantKpiResponse plantKpis(@RequestParam Long plantId,
                                      @AuthenticationPrincipal MipUserDetails principal) {
        return kpiService.plantKpis(plantId, principal);
    }
}
