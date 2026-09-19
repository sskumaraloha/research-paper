package com.mip.plant.service;

import com.mip.exception.ResourceNotFoundException;
import com.mip.exception.ValidationException;
import com.mip.plant.dto.LineResponse;
import com.mip.plant.dto.PlantSettingsResponse;
import com.mip.plant.dto.PlantSummaryResponse;
import com.mip.plant.dto.UpdatePlantSettingsRequest;
import com.mip.plant.entity.Plant;
import com.mip.plant.repository.PlantRepository;
import com.mip.plant.repository.ProductionLineRepository;
import com.mip.security.MipUserDetails;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlantService {

    private final PlantRepository plantRepository;
    private final ProductionLineRepository lineRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PlantSummaryResponse> listPlantsForUser(MipUserDetails principal) {
        return accessiblePlants(principal).stream()
                .sorted(Comparator.comparing(Plant::getName))
                .map(p -> new PlantSummaryResponse(p.getId(), p.getCode(), p.getName(), p.getLocation(),
                        p.getOrganisation() == null ? null : p.getOrganisation().getName()))
                .toList();
    }

    /** Threshold changes; the caller enforces the ADMIN role. */
    @Transactional
    public PlantSettingsResponse updatePlantSettings(Long plantId, UpdatePlantSettingsRequest request,
                                                     MipUserDetails principal) {
        Plant plant = requireAccessiblePlant(plantId, principal);
        if (request.lowConfidenceThreshold() >= request.autoApproveThreshold()) {
            throw new ValidationException("Invalid thresholds", Map.of("lowConfidenceThreshold",
                    "must be below autoApproveThreshold"));
        }
        plant.setAutoApproveThreshold(request.autoApproveThreshold());
        plant.setLowConfidenceThreshold(request.lowConfidenceThreshold());
        plant.setDowntimeAlertMinutes(request.downtimeAlertMinutes());
        return new PlantSettingsResponse(plant.getId(), plant.getName(),
                plant.getAutoApproveThreshold(), plant.getLowConfidenceThreshold(),
                plant.getDowntimeAlertMinutes());
    }

    @Transactional(readOnly = true)
    public PlantSettingsResponse getPlantSettings(Long plantId, MipUserDetails principal) {
        Plant plant = requireAccessiblePlant(plantId, principal);
        return new PlantSettingsResponse(plant.getId(), plant.getName(),
                plant.getAutoApproveThreshold(), plant.getLowConfidenceThreshold(),
                plant.getDowntimeAlertMinutes());
    }

    @Transactional(readOnly = true)
    public List<LineResponse> listLines(Long plantId, MipUserDetails principal) {
        requireAccessiblePlant(plantId, principal);
        return lineRepository.findByPlantIdOrderByCodeAsc(plantId).stream()
                .map(l -> new LineResponse(l.getId(), l.getCode(), l.getName()))
                .toList();
    }

    /**
     * Loads a plant the principal may access. Admins see every plant; everyone else only
     * plants assigned to their account. Inaccessible plants are reported as not found so
     * plant ids cannot be probed.
     */
    @Transactional(readOnly = true)
    public Plant requireAccessiblePlant(Long plantId, MipUserDetails principal) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plant", plantId));
        if (!isAdmin(principal)) {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
            boolean assigned = user.getPlants().stream().anyMatch(p -> p.getId().equals(plantId));
            if (!assigned) {
                throw new ResourceNotFoundException("Plant", plantId);
            }
        }
        return plant;
    }

    @Transactional(readOnly = true)
    public List<Plant> accessiblePlants(MipUserDetails principal) {
        if (isAdmin(principal)) {
            return plantRepository.findAll();
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        return List.copyOf(user.getPlants());
    }

    private boolean isAdmin(MipUserDetails principal) {
        return RoleName.ADMIN.name().equals(principal.getRole());
    }
}
