package com.mip.insight.repository;

import com.mip.insight.entity.InsightEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsightEvidenceRepository extends JpaRepository<InsightEvidence, Long> {

    List<InsightEvidence> findByInsightId(Long insightId);
}
