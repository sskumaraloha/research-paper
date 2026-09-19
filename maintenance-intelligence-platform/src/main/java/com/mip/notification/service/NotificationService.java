package com.mip.notification.service;

import com.mip.common.dto.PageResponse;
import com.mip.exception.ResourceNotFoundException;
import com.mip.importjob.entity.ImportJob;
import com.mip.importjob.repository.ImportJobRepository;
import com.mip.insight.repository.InsightRepository;
import com.mip.notification.dto.BadgeCountsResponse;
import com.mip.notification.dto.NotificationResponse;
import com.mip.notification.entity.Notification;
import com.mip.notification.repository.NotificationRepository;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.security.MipUserDetails;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.user.repository.UserRepository;
import com.mip.validation.entity.ValidationItem;
import com.mip.validation.repository.ValidationItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ValidationItemRepository validationItemRepository;
    private final InsightRepository insightRepository;
    private final ImportJobRepository importJobRepository;
    private final PlantService plantService;

    // --- user-facing reads ---

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(int page, int size, MipUserDetails principal) {
        return PageResponse.of(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        principal.getId(), PageRequest.of(page, Math.min(size, 100))),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(MipUserDetails principal) {
        return notificationRepository.countByUserIdAndReadFlagFalse(principal.getId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, MipUserDetails principal) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.setReadFlag(true);
        return toResponse(notification);
    }

    @Transactional
    public long markAllRead(MipUserDetails principal) {
        return notificationRepository.markAllRead(principal.getId());
    }

    @Transactional(readOnly = true)
    public BadgeCountsResponse badgeCounts(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return new BadgeCountsResponse(
                validationItemRepository.countByPlantIdAndStatus(plantId,
                        ValidationItem.ValidationStatus.PENDING),
                notificationRepository.countByUserIdAndReadFlagFalse(principal.getId()),
                insightRepository.countByPlantId(plantId));
    }

    // --- event hooks called by other modules ---

    /** Takes the id and re-loads inside this transaction: callers hold a detached job. */
    @Transactional
    public void onValidationPending(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getNeedsValidationCount() == 0) {
            return;
        }
        notifyPlantStaff(job.getPlant(), null, Notification.NotificationType.VALIDATION_PENDING,
                "Import needs review",
                job.getNeedsValidationCount() + " rows from '" + job.getSourceDocument().getFilename()
                        + "' are waiting in the validation queue.",
                "IMPORT_JOB", job.getId());
    }

    @Transactional
    public void onInsightChanged(Plant plant, long criticalCount) {
        if (criticalCount == 0) {
            return;
        }
        notifyPlantStaff(plant, null, Notification.NotificationType.INSIGHT,
                "Critical insights detected",
                criticalCount + " critical insight(s) are active for " + plant.getName() + ".",
                "PLANT", plant.getId());
    }

    @Transactional
    public void onMaintenanceOverdue(com.mip.schedule.entity.MaintenanceSchedule schedule) {
        notifyPlantStaff(schedule.getPlant(), null, Notification.NotificationType.MAINTENANCE_DUE,
                "Preventive maintenance overdue",
                "'" + schedule.getTitle() + "' on " + schedule.getMachine().getName()
                        + " was due on " + schedule.getNextDueOn() + ".",
                "SCHEDULE", schedule.getId());
    }

    @Transactional
    public void onRecordCreated(MaintenanceRecord record) {
        Long creatorId = record.getCreatedBy() == null ? null : record.getCreatedBy().getId();
        notifyPlantStaff(record.getPlant(), creatorId, Notification.NotificationType.RECORD_CREATED,
                "New maintenance record",
                record.getMachine().getName() + ": " + snippet(record.getDescription()),
                "RECORD", record.getId());
    }

    /** Active engineers assigned to the plant plus every active admin, minus the actor. */
    private void notifyPlantStaff(Plant plant, Long excludeUserId,
                                  Notification.NotificationType type, String title, String message,
                                  String entityType, Long entityId) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        for (User user : userRepository.findByActiveTrueAndPlantsId(plant.getId())) {
            if (user.getRole() != RoleName.VIEWER) {
                recipients.put(user.getId(), user);
            }
        }
        for (User admin : userRepository.findByActiveTrueAndRole(RoleName.ADMIN)) {
            recipients.put(admin.getId(), admin);
        }
        if (excludeUserId != null) {
            recipients.remove(excludeUserId);
        }
        List<Notification> notifications = recipients.values().stream()
                .map(user -> new Notification(user, plant, type, title, message, entityType, entityId))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType().name(),
                notification.getTitle(), notification.getMessage(), notification.getEntityType(),
                notification.getEntityId(), notification.isReadFlag(), notification.getCreatedAt());
    }

    private String snippet(String text) {
        return text.length() <= 120 ? text : text.substring(0, 117) + "...";
    }
}
