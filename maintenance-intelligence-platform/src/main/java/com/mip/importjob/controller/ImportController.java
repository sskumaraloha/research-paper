package com.mip.importjob.controller;

import com.mip.importjob.dto.ImportJobResponse;
import com.mip.importjob.dto.ImportSummaryResponse;
import com.mip.importjob.service.ImportService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ImportSummaryResponse uploadFile(@RequestParam Long plantId,
                                            @RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal MipUserDetails principal) {
        return importService.uploadFile(plantId, file, principal);
    }

    @GetMapping("/latest")
    public ImportJobResponse getLatestJob(@RequestParam Long plantId,
                                          @AuthenticationPrincipal MipUserDetails principal) {
        return importService.getLatestJob(plantId, principal);
    }

    @GetMapping("/{jobId}")
    public ImportJobResponse getJob(@PathVariable Long jobId,
                                    @AuthenticationPrincipal MipUserDetails principal) {
        return importService.getJob(jobId, principal);
    }

    @PostMapping("/{jobId}/rerun")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ImportJobResponse rerunJob(@PathVariable Long jobId,
                                      @AuthenticationPrincipal MipUserDetails principal) {
        return importService.rerunJob(jobId, principal);
    }
}
