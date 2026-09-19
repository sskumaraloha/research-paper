package com.mip.assistant.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A user's Q&A thread with the assistant; the title is the first question asked. */
@Entity
@Table(name = "assistant_conversations")
@Getter
@Setter
@NoArgsConstructor
public class AssistantConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(nullable = false, length = 150)
    private String title;

    public AssistantConversation(User user, Plant plant, String title) {
        this.user = user;
        this.plant = plant;
        this.title = title;
    }
}
