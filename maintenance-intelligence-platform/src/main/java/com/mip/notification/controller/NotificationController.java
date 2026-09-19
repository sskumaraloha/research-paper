package com.mip.notification.controller;

import com.mip.common.dto.CountResponse;
import com.mip.common.dto.PageResponse;
import com.mip.notification.dto.BadgeCountsResponse;
import com.mip.notification.dto.NotificationResponse;
import com.mip.notification.service.NotificationService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> listForUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MipUserDetails principal) {
        return notificationService.listForUser(page, size, principal);
    }

    @GetMapping("/unread-count")
    public CountResponse unreadCount(@AuthenticationPrincipal MipUserDetails principal) {
        return new CountResponse(notificationService.unreadCount(principal));
    }

    @GetMapping("/badge-counts")
    public BadgeCountsResponse badgeCounts(@RequestParam Long plantId,
                                           @AuthenticationPrincipal MipUserDetails principal) {
        return notificationService.badgeCounts(plantId, principal);
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId,
                                         @AuthenticationPrincipal MipUserDetails principal) {
        return notificationService.markRead(notificationId, principal);
    }

    @PostMapping("/read-all")
    public CountResponse markAllRead(@AuthenticationPrincipal MipUserDetails principal) {
        return new CountResponse(notificationService.markAllRead(principal));
    }
}
