package com.mip.entry.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A guided conversation that collects one maintenance record's fields. The draft
 * lives in columns so a conversation survives restarts and can be audited.
 */
@Entity
@Table(name = "entry_conversations")
@Getter
@Setter
@NoArgsConstructor
public class EntryConversation extends BaseEntity {

    public enum ConversationStatus {COLLECTING, AWAITING_CONFIRMATION, CONFIRMED, CANCELLED}

    public enum Channel {WEB, WHATSAPP}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ConversationStatus status = ConversationStatus.COLLECTING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel = Channel.WEB;

    // --- draft record fields ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Column(length = 200)
    private String machineText;

    private LocalDate recordDate;

    private Integer downtimeMinutes;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String actionTaken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_mode_id")
    private FailureMode failureMode;

    @Column(length = 500)
    private String partsText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_record_id")
    private MaintenanceRecord resultingRecord;

    public EntryConversation(User user, Plant plant) {
        this.user = user;
        this.plant = plant;
    }

    public EntryConversation(User user, Plant plant, Channel channel) {
        this(user, plant);
        this.channel = channel;
    }
}
