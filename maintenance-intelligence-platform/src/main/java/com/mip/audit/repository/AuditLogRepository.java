package com.mip.audit.repository;

import com.mip.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:plantId is null or a.plantId = :plantId)
              and (:action is null or a.action = :action)
            order by a.createdAt desc, a.id desc
            """)
    Page<AuditLog> search(@Param("plantId") Long plantId,
                          @Param("action") String action,
                          Pageable pageable);
}
