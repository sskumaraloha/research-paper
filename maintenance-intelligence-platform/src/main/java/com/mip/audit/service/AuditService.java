package com.mip.audit.service;

import com.mip.audit.dto.AuditLogResponse;
import com.mip.audit.entity.AuditLog;
import com.mip.audit.repository.AuditLogRepository;
import com.mip.common.dto.PageResponse;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an action in the caller's transaction: if the action rolls back, so does
     * its trail entry — the log never claims something that did not happen.
     */
    @Transactional
    public void log(MipUserDetails actor, String action, String entityType, Long entityId,
                    Long plantId, String detail) {
        log(actor.getId(), actor.getFullName(), action, entityType, entityId, plantId, detail);
    }

    @Transactional
    public void log(Long actorId, String actorName, String action, String entityType,
                    Long entityId, Long plantId, String detail) {
        auditLogRepository.save(new AuditLog(actorId, actorName, action, entityType, entityId,
                plantId, truncate(detail)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(Long plantId, String action, int page, int size) {
        String actionFilter = (action == null || action.isBlank()) ? null : action.trim();
        return PageResponse.of(auditLogRepository.search(plantId, actionFilter,
                        PageRequest.of(page, Math.min(size, 200))),
                a -> new AuditLogResponse(a.getId(), a.getActorId(), a.getActorName(), a.getAction(),
                        a.getEntityType(), a.getEntityId(), a.getPlantId(), a.getDetail(),
                        a.getCreatedAt()));
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= 500 ? detail : detail.substring(0, 497) + "...";
    }
}
