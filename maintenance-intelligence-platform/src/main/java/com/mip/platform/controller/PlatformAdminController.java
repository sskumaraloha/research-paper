package com.mip.platform.controller;

import com.mip.platform.dto.OrganisationDetailResponse;
import com.mip.platform.dto.OrganisationStatsResponse;
import com.mip.platform.dto.PlatformOverviewResponse;
import com.mip.platform.service.PlatformAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The software owner's console. PLATFORM_ADMIN only — customer admins outrank
 * engineers but never see across organisations.
 */
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    @GetMapping("/overview")
    public PlatformOverviewResponse overview() {
        return platformAdminService.overview();
    }

    @GetMapping("/organisations")
    public List<OrganisationStatsResponse> listOrganisations() {
        return platformAdminService.listOrganisations();
    }

    @GetMapping("/organisations/{organisationId}")
    public OrganisationDetailResponse getOrganisation(@PathVariable Long organisationId) {
        return platformAdminService.getOrganisation(organisationId);
    }
}
