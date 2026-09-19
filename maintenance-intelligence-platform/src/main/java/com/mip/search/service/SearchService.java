package com.mip.search.service;

import com.mip.common.entity.BaseEntity;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.repository.FailureModeRepository;
import com.mip.exception.InvalidRequestException;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.MachineRepository;
import com.mip.part.entity.SparePart;
import com.mip.part.repository.SparePartRepository;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.search.dto.GlobalSearchResponse;
import com.mip.search.dto.RecordMatchResponse;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int SECTION_LIMIT = 5;

    private final MachineRepository machineRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final SparePartRepository sparePartRepository;
    private final FailureModeRepository failureModeRepository;
    private final QueryNormalizerService queryNormalizerService;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public GlobalSearchResponse globalSearch(String query, MipUserDetails principal) {
        requireQuery(query);
        List<Long> plantIds = accessiblePlantIds(principal);
        Set<String> variants = queryNormalizerService.expandTerms(query);

        return new GlobalSearchResponse(query,
                searchMachines(variants, plantIds),
                searchRecordMatches(variants, plantIds),
                searchParts(variants),
                searchFailureModes(variants));
    }

    @Transactional(readOnly = true)
    public List<RecordMatchResponse> searchRecords(String query, MipUserDetails principal) {
        requireQuery(query);
        return searchRecordMatches(queryNormalizerService.expandTerms(query),
                accessiblePlantIds(principal));
    }

    @Transactional(readOnly = true)
    public List<GlobalSearchResponse.MachineMatch> searchMachines(String query,
                                                                  MipUserDetails principal) {
        requireQuery(query);
        return searchMachines(queryNormalizerService.expandTerms(query), accessiblePlantIds(principal));
    }

    private List<GlobalSearchResponse.MachineMatch> searchMachines(Set<String> variants,
                                                                   List<Long> plantIds) {
        if (plantIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Machine> matches = new LinkedHashMap<>();
        for (String variant : variants) {
            for (Machine machine : machineRepository.searchAcrossPlants(plantIds, variant,
                    PageRequest.of(0, SECTION_LIMIT))) {
                matches.putIfAbsent(machine.getId(), machine);
            }
            if (matches.size() >= SECTION_LIMIT) {
                break;
            }
        }
        return matches.values().stream()
                .limit(SECTION_LIMIT)
                .map(m -> new GlobalSearchResponse.MachineMatch(m.getId(), m.getCode(), m.getName(),
                        m.getPlant().getName()))
                .toList();
    }

    private List<RecordMatchResponse> searchRecordMatches(Set<String> variants, List<Long> plantIds) {
        if (plantIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MaintenanceRecord> matches = new LinkedHashMap<>();
        for (String variant : variants) {
            for (MaintenanceRecord record : recordRepository.fullTextAcrossPlants(plantIds, variant,
                    PageRequest.of(0, SECTION_LIMIT * 2))) {
                matches.putIfAbsent(record.getId(), record);
            }
        }
        return matches.values().stream()
                .sorted(Comparator.comparing(MaintenanceRecord::getRecordDate).reversed())
                .limit(SECTION_LIMIT * 2L)
                .map(r -> new RecordMatchResponse(r.getId(), r.getRecordDate(),
                        r.getMachine().getName(),
                        r.getFailureMode() == null ? null : r.getFailureMode().getName(),
                        r.getDowntimeMinutes(), snippet(r.getDescription())))
                .toList();
    }

    private List<GlobalSearchResponse.PartMatch> searchParts(Set<String> variants) {
        Map<Long, SparePart> matches = new LinkedHashMap<>();
        for (String variant : variants) {
            for (SparePart part : sparePartRepository.search(variant,
                    PageRequest.of(0, SECTION_LIMIT))) {
                matches.putIfAbsent(part.getId(), part);
            }
        }
        return matches.values().stream()
                .limit(SECTION_LIMIT)
                .map(p -> new GlobalSearchResponse.PartMatch(p.getId(), p.getPartNumber(), p.getName()))
                .toList();
    }

    private List<GlobalSearchResponse.FailureModeMatch> searchFailureModes(Set<String> variants) {
        List<GlobalSearchResponse.FailureModeMatch> matches = new ArrayList<>();
        for (FailureMode mode : failureModeRepository.findAllByOrderByNameAsc()) {
            String name = mode.getName().toLowerCase(Locale.ROOT);
            boolean hit = variants.stream().anyMatch(v -> name.contains(v) || v.contains(name)
                    || mode.keywordList().stream().anyMatch(v::contains));
            if (hit) {
                matches.add(new GlobalSearchResponse.FailureModeMatch(mode.getId(), mode.getCode(),
                        mode.getName()));
            }
        }
        return matches.stream().limit(SECTION_LIMIT).toList();
    }

    private List<Long> accessiblePlantIds(MipUserDetails principal) {
        return plantService.accessiblePlants(principal).stream().map(BaseEntity::getId).toList();
    }

    private void requireQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidRequestException("Search query must not be empty");
        }
    }

    private String snippet(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 160 ? text : text.substring(0, 157) + "...";
    }
}
