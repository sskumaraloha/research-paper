package com.mip.assistant.entity;

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
@Table(name = "assistant_messages")
@Getter
@Setter
@NoArgsConstructor
public class AssistantMessage extends BaseEntity {

    public enum Sender {USER, ASSISTANT}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AssistantConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sender sender;

    @Column(nullable = false, length = 4000)
    private String content;

    /** The routed intent, recorded on assistant messages for later analysis. */
    @Column(length = 25)
    private String intent;

    public AssistantMessage(AssistantConversation conversation, Sender sender, String content,
                            String intent) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.intent = intent;
    }
}
