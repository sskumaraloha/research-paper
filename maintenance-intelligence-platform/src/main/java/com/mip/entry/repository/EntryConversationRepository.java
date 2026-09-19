package com.mip.entry.repository;

import com.mip.entry.entity.EntryConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntryConversationRepository extends JpaRepository<EntryConversation, Long> {

    Optional<EntryConversation> findByIdAndUserId(Long id, Long userId);

    Optional<EntryConversation> findTopByUserIdAndChannelOrderByIdDesc(
            Long userId, EntryConversation.Channel channel);
}
