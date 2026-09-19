package com.mip.insight.controller;

import com.mip.common.dto.CountResponse;
import com.mip.insight.dto.InsightResponse;
import com.mip.insight.service.InsightService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @GetMapping
    public List<InsightResponse> listInsights(@RequestParam Long plantId,
                                              @AuthenticationPrincipal MipUserDetails principal) {
        return insightService.listInsights(plantId, principal);
    }

    @GetMapping("/count")
    public CountResponse insightCount(@RequestParam Long plantId,
                                      @AuthenticationPrincipal MipUserDetails principal) {
        return new CountResponse(insightService.insightCount(plantId, principal));
    }

    @PostMapping("/recompute")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public List<InsightResponse> recomputeAll(@RequestParam Long plantId,
                                              @AuthenticationPrincipal MipUserDetails principal) {
        return insightService.recomputeAll(plantId, principal);
    }
}
