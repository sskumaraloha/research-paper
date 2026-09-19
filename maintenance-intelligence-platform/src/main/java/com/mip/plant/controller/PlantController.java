package com.mip.plant.controller;

import com.mip.plant.dto.LineResponse;
import com.mip.plant.dto.PlantSettingsResponse;
import com.mip.plant.dto.PlantSummaryResponse;
import com.mip.plant.service.PlantService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    @GetMapping
    public List<PlantSummaryResponse> listPlants(@AuthenticationPrincipal MipUserDetails principal) {
        return plantService.listPlantsForUser(principal);
    }

    @GetMapping("/{plantId}/settings")
    public PlantSettingsResponse getSettings(@PathVariable Long plantId,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return plantService.getPlantSettings(plantId, principal);
    }

    @GetMapping("/{plantId}/lines")
    public List<LineResponse> listLines(@PathVariable Long plantId,
                                        @AuthenticationPrincipal MipUserDetails principal) {
        return plantService.listLines(plantId, principal);
    }
}
