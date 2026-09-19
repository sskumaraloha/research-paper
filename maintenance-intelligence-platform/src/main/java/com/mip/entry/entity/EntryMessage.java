package com.mip.entry.entity;

import com.mip.common.entity.BaseEntity;
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

@Entity
@Table(name = "entry_messages")
@Getter
@Setter
@NoArgsConstructor
public class EntryMessage extends BaseEntity {

    public enum Sender {USER, ASSISTANT}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private EntryConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sender sender;

    @Column(nullable = false, length = 1000)
    private String content;

    public EntryMessage(EntryConversation conversation, Sender sender, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
    }
}
