package com.mip.record.mapper;

import com.mip.common.dto.NamedRef;
import com.mip.record.dto.RecordDetailResponse;
import com.mip.record.dto.RecordRowResponse;
import com.mip.record.entity.MaintenanceRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RecordMapper {

    public RecordRowResponse toRow(MaintenanceRecord record) {
        return new RecordRowResponse(
                record.getId(), record.getRecordDate(),
                record.getMachine().getId(), record.getMachine().getCode(), record.getMachine().getName(),
                record.getFailureMode() == null ? null : record.getFailureMode().getName(),
                record.getFailureMode() == null ? null : record.getFailureMode().getCategory().name(),
                record.getDowntimeMinutes(), record.getTechnician(),
                record.getSource().name(), record.getStatus().name(), record.getConfidence(),
                record.getDescription());
    }

    public RecordDetailResponse toDetail(MaintenanceRecord record) {
        List<NamedRef> parts = record.getSpareParts().stream()
                .map(p -> new NamedRef(p.getId(), p.getName()))
                .sorted(Comparator.comparing(NamedRef::name))
                .toList();
        return new RecordDetailResponse(
                record.getId(), record.getPlant().getId(), record.getRecordDate(),
                record.getMachine().getId(), record.getMachine().getCode(), record.getMachine().getName(),
                record.getMachine().getLine() == null ? null : record.getMachine().getLine().getName(),
                record.getFailureMode() == null ? null : record.getFailureMode().getId(),
                record.getFailureMode() == null ? null : record.getFailureMode().getName(),
                record.getFailureMode() == null ? null : record.getFailureMode().getCategory().name(),
                record.getDowntimeMinutes(), record.getDescription(), record.getActionTaken(),
                record.getTechnician(), record.getSource().name(), record.getStatus().name(),
                record.getConfidence(), record.getRejectedReason(), parts,
                record.getSourceDocument() == null ? null : record.getSourceDocument().getId(),
                record.getSourceDocument() == null ? null : record.getSourceDocument().getFilename(),
                record.getCreatedBy() == null ? null : record.getCreatedBy().getFullName());
    }
}
