package com.mip.record.controller;

import com.mip.common.dto.PageResponse;
import com.mip.record.dto.CreateRecordRequest;
import com.mip.record.dto.RecordDetailResponse;
import com.mip.record.dto.RecordFilterOptionsResponse;
import com.mip.record.dto.RecordRowResponse;
import com.mip.record.dto.RejectRequest;
import com.mip.record.dto.SourceDocumentResponse;
import com.mip.record.service.MaintenanceRecordService;
import com.mip.security.MipUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService recordService;

    @GetMapping
    public PageResponse<RecordRowResponse> listRecords(
            @RequestParam Long plantId,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long failureModeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.listRecords(plantId, machineId, lineId, failureModeId, from, to,
                text, page, size, principal);
    }

    @GetMapping("/filter-options")
    public RecordFilterOptionsResponse listFilterOptions(@RequestParam Long plantId,
                                                         @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.listFilterOptions(plantId, principal);
    }

    @GetMapping("/source-documents")
    public List<SourceDocumentResponse> listSourceDocuments(@RequestParam Long plantId,
                                                            @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.listSourceDocuments(plantId, principal);
    }

    @GetMapping("/{recordId}")
    public RecordDetailResponse getRecord(@PathVariable Long recordId,
                                          @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.getRecord(recordId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public RecordDetailResponse createRecord(@Valid @RequestBody CreateRecordRequest request,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.createRecord(request, principal);
    }

    @PostMapping("/{recordId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public RecordDetailResponse rejectRecord(@PathVariable Long recordId,
                                             @Valid @RequestBody RejectRequest request,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return recordService.rejectRecord(recordId, request, principal);
    }
}
