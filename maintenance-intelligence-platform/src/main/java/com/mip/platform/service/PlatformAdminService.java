package com.mip.platform.service;

import com.mip.common.entity.BaseEntity;
import com.mip.exception.ResourceNotFoundException;
import com.mip.importjob.repository.ImportJobRepository;
import com.mip.machine.repository.MachineRepository;
import com.mip.plant.entity.Organisation;
import com.mip.plant.entity.Plant;
import com.mip.plant.repository.OrganisationRepository;
import com.mip.plant.repository.PlantRepository;
import com.mip.platform.dto.OrganisationDetailResponse;
import com.mip.platform.dto.OrganisationStatsResponse;
import com.mip.platform.dto.PlatformOverviewResponse;
import com.mip.record.entity.RecordStatus;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.user.dto.UserSummaryResponse;
import com.mip.user.entity.User;
import com.mip.user.repository.UserRepository;
import com.mip.validation.entity.ValidationItem;
import com.mip.validation.repository.ValidationItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cross-organisation oversight for the software owner (PLATFORM_ADMIN only). */
@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private final OrganisationRepository organisationRepository;
    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final ImportJobRepository importJobRepository;
    private final ValidationItemRepository validationItemRepository;

    @Transactional(readOnly = true)
    public PlatformOverviewResponse overview() {
        return new PlatformOverviewResponse(
                organisationRepository.count(),
                plantRepository.count(),
                userRepository.count(),
                machineRepository.count(),
                recordRepository.count(),
                recordRepository.countByStatusAndRecordDateGreaterThanEqual(RecordStatus.ACTIVE,
                        LocalDate.now().minusDays(30)),
                importJobRepository.count(),
                validationItemRepository.countByStatus(ValidationItem.ValidationStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public List<OrganisationStatsResponse> listOrganisations() {
        List<OrganisationStatsResponse> stats = new ArrayList<>();
        for (Organisation organisation : organisationRepository.findAll()) {
            List<Long> plantIds = plantRepository
                    .findByOrganisationIdOrderByNameAsc(organisation.getId()).stream()
                    .map(BaseEntity::getId).toList();
            stats.add(new OrganisationStatsResponse(
                    organisation.getId(), organisation.getCode(), organisation.getName(),
                    plantIds.size(),
                    plantIds.isEmpty() ? 0 : userRepository.countDistinctByPlantIds(plantIds),
                    plantIds.isEmpty() ? 0 : machineRepository.countByPlantIdIn(plantIds),
                    plantIds.isEmpty() ? 0 : recordRepository.countByPlantIdInAndStatus(plantIds,
                            RecordStatus.ACTIVE),
                    plantIds.isEmpty() ? 0 : importJobRepository.countByPlantIdIn(plantIds),
                    plantIds.isEmpty() ? null : recordRepository.lastActivityForPlants(plantIds)));
        }
        stats.sort(Comparator.comparing(OrganisationStatsResponse::name));
        return stats;
    }

    @Transactional(readOnly = true)
    public OrganisationDetailResponse getOrganisation(Long organisationId) {
        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", organisationId));
        List<Plant> plants = plantRepository.findByOrganisationIdOrderByNameAsc(organisationId);
        LocalDate windowFrom = LocalDate.now().minusDays(29);

        List<OrganisationDetailResponse.PlantStats> plantStats = plants.stream()
                .map(plant -> new OrganisationDetailResponse.PlantStats(
                        plant.getId(), plant.getCode(), plant.getName(),
                        machineRepository.countByPlantId(plant.getId()),
                        recordRepository.countByPlantIdAndStatus(plant.getId(), RecordStatus.ACTIVE),
                        recordRepository.totalDowntimeBetween(plant.getId(), windowFrom,
                                LocalDate.now()),
                        validationItemRepository.countByPlantIdAndStatus(plant.getId(),
                                ValidationItem.ValidationStatus.PENDING)))
                .toList();

        List<Long> plantIds = plants.stream().map(BaseEntity::getId).toList();
        List<UserSummaryResponse> users = plantIds.isEmpty() ? List.of()
                : userRepository.findDistinctByPlantIds(plantIds).stream()
                        .map(this::toSummary)
                        .toList();

        return new OrganisationDetailResponse(organisation.getId(), organisation.getCode(),
                organisation.getName(), plantStats, users);
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getRole().name(), user.isActive(),
                user.getPlants().stream().map(BaseEntity::getId).sorted().toList());
    }
}
