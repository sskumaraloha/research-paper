package com.mip.entry.repository;

import com.mip.entry.entity.EntryMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryMessageRepository extends JpaRepository<EntryMessage, Long> {

    List<EntryMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
}
