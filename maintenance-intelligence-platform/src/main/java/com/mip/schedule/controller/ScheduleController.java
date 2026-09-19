package com.mip.schedule.controller;

import com.mip.schedule.dto.CompleteScheduleRequest;
import com.mip.schedule.dto.CreateScheduleRequest;
import com.mip.schedule.dto.ScheduleResponse;
import com.mip.schedule.dto.UpdateScheduleRequest;
import com.mip.schedule.service.MaintenanceScheduleService;
import com.mip.security.MipUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final MaintenanceScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> listSchedules(
            @RequestParam Long plantId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal MipUserDetails principal) {
        return scheduleService.listSchedules(plantId, includeInactive, principal);
    }

    @GetMapping("/due")
    public List<ScheduleResponse> listDue(@RequestParam Long plantId,
                                          @AuthenticationPrincipal MipUserDetails principal) {
        return scheduleService.listDue(plantId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ScheduleResponse createSchedule(@Valid @RequestBody CreateScheduleRequest request,
                                           @AuthenticationPrincipal MipUserDetails principal) {
        return scheduleService.createSchedule(request, principal);
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ScheduleResponse updateSchedule(@PathVariable Long scheduleId,
                                           @Valid @RequestBody UpdateScheduleRequest request,
                                           @AuthenticationPrincipal MipUserDetails principal) {
        return scheduleService.updateSchedule(scheduleId, request, principal);
    }

    @PostMapping("/{scheduleId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ScheduleResponse completeSchedule(@PathVariable Long scheduleId,
                                             @Valid @RequestBody CompleteScheduleRequest request,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return scheduleService.completeSchedule(scheduleId, request, principal);
    }
}
