package com.mip.notification.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications",
        indexes = @Index(name = "ix_notification_user_read", columnList = "user_id, readFlag"))
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    public enum NotificationType {VALIDATION_PENDING, INSIGHT, RECORD_CREATED, MAINTENANCE_DUE}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** What the notification points at, for client-side navigation. */
    @Column(length = 30)
    private String entityType;

    private Long entityId;

    @Column(nullable = false)
    private boolean readFlag = false;

    public Notification(User user, Plant plant, NotificationType type, String title,
                        String message, String entityType, Long entityId) {
        this.user = user;
        this.plant = plant;
        this.type = type;
        this.title = title;
        this.message = message;
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
