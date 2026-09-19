package com.mip.schedule.config;

import com.mip.schedule.service.MaintenanceScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the overdue-maintenance notification sweep every morning at 06:00 server time. */
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleSweepJob {

    private final MaintenanceScheduleService scheduleService;

    @Scheduled(cron = "0 0 6 * * *")
    public void notifyOverdueSchedules() {
        scheduleService.notifyOverdueSchedules();
    }
}
