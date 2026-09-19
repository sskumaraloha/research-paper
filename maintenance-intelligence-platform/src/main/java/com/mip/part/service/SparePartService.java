package com.mip.part.service;

import com.mip.common.dto.NamedRef;
import com.mip.common.dto.PageResponse;
import com.mip.common.entity.BaseEntity;
import com.mip.exception.InvalidRequestException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.part.dto.PartDetailResponse;
import com.mip.part.dto.PartRowResponse;
import com.mip.part.entity.SparePart;
import com.mip.part.repository.SparePartRepository;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.mapper.RecordMapper;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.record.repository.PartUsageProjection;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SparePartService {

    private final SparePartRepository sparePartRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final PlantService plantService;
    private final RecordMapper recordMapper;

    @Transactional(readOnly = true)
    public PageResponse<PartRowResponse> listParts(String query, int page, int size,
                                                   MipUserDetails principal) {
        List<Long> plantIds = accessiblePlantIds(principal);
        String textFilter = (query == null || query.isBlank()) ? null : query.trim();
        Page<SparePart> parts = sparePartRepository.search(textFilter,
                PageRequest.of(page, Math.min(size, 100), Sort.by("name")));

        List<Long> partIds = parts.getContent().stream().map(BaseEntity::getId).toList();
        Map<Long, PartUsageProjection> usage = partIds.isEmpty() || plantIds.isEmpty() ? Map.of()
                : recordRepository.partUsage(partIds, plantIds).stream()
                        .collect(Collectors.toMap(PartUsageProjection::getPartId, Function.identity()));

        return PageResponse.of(parts.map(part -> {
            PartUsageProjection u = usage.get(part.getId());
            return new PartRowResponse(part.getId(), part.getPartNumber(), part.getName(),
                    part.getCategory(),
                    u == null ? 0 : u.getUsageCount(),
                    u == null ? null : u.getLastUsedDate());
        }));
    }

    @Transactional(readOnly = true)
    public PartDetailResponse getPartDetail(Long partId, MipUserDetails principal) {
        SparePart part = sparePartRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Spare part", partId));
        List<Long> plantIds = accessiblePlantIds(principal);
        List<MaintenanceRecord> usages = plantIds.isEmpty() ? List.of()
                : recordRepository.findByPartUsage(partId, plantIds);

        Double avgIntervalDays = averageReplacementIntervalDays(usages);
        Set<NamedRef> machines = new LinkedHashSet<>();
        for (MaintenanceRecord record : usages) {
            machines.add(new NamedRef(record.getMachine().getId(), record.getMachine().getName()));
        }
        List<MaintenanceRecord> recent = new ArrayList<>(usages);
        recent.sort(Comparator.comparing(MaintenanceRecord::getRecordDate).reversed());

        return new PartDetailResponse(part.getId(), part.getPartNumber(), part.getName(),
                part.getCategory(), usages.size(),
                usages.isEmpty() ? null : usages.get(usages.size() - 1).getRecordDate(),
                avgIntervalDays, List.copyOf(machines),
                recent.stream().limit(10).map(recordMapper::toRow).toList());
    }

    /**
     * Finds a part by number or name; creates it when unknown. Used by manual record
     * entry, the import pipeline and the entry agent so part identity stays unified.
     */
    @Transactional
    public SparePart resolveOrCreatePart(String nameOrNumber) {
        if (nameOrNumber == null || nameOrNumber.isBlank()) {
            throw new InvalidRequestException("Part name must not be blank");
        }
        String needle = nameOrNumber.trim();
        return sparePartRepository.findByPartNumberIgnoreCase(needle)
                .or(() -> sparePartRepository.findByNameIgnoreCase(needle))
                .orElseGet(() -> sparePartRepository.save(
                        new SparePart(generatePartNumber(needle), needle, null)));
    }

    /** Days between first and last usage divided by the number of intervals. */
    static Double averageReplacementIntervalDays(List<MaintenanceRecord> usagesOldestFirst) {
        if (usagesOldestFirst.size() < 2) {
            return null;
        }
        long totalDays = ChronoUnit.DAYS.between(
                usagesOldestFirst.get(0).getRecordDate(),
                usagesOldestFirst.get(usagesOldestFirst.size() - 1).getRecordDate());
        return (double) totalDays / (usagesOldestFirst.size() - 1);
    }

    private String generatePartNumber(String name) {
        String base = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-");
        base = base.length() > 20 ? base.substring(0, 20) : base;
        String candidate = "PRT-" + base;
        int suffix = 2;
        while (sparePartRepository.findByPartNumberIgnoreCase(candidate).isPresent()) {
            candidate = "PRT-" + base + "-" + suffix++;
        }
        return candidate;
    }

    private List<Long> accessiblePlantIds(MipUserDetails principal) {
        return plantService.accessiblePlants(principal).stream()
                .map(Plant::getId)
                .toList();
    }
}
