package com.mip.notification.repository;

import com.mip.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFlagFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.user.id = :userId and n.readFlag = false")
    int markAllRead(@Param("userId") Long userId);
}
