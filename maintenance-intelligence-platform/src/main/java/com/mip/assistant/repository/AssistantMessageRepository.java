package com.mip.assistant.repository;

import com.mip.assistant.entity.AssistantMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, Long> {

    List<AssistantMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
}
