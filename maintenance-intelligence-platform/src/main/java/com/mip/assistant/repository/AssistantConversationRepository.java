package com.mip.assistant.repository;

import com.mip.assistant.entity.AssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, Long> {

    Optional<AssistantConversation> findByIdAndUserId(Long id, Long userId);

    List<AssistantConversation> findTop20ByUserIdOrderByUpdatedAtDesc(Long userId);
}
