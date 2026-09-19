package com.mip.record.repository;

import com.mip.record.entity.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {

    List<SourceDocument> findByPlantIdOrderByCreatedAtDesc(Long plantId);

    Optional<SourceDocument> findByPlantIdAndChecksum(Long plantId, String checksum);
}
