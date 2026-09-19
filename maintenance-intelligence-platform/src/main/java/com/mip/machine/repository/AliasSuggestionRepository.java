package com.mip.machine.repository;

import com.mip.machine.entity.AliasSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AliasSuggestionRepository extends JpaRepository<AliasSuggestion, Long> {

    Optional<AliasSuggestion> findByPlantIdAndRawText(Long plantId, String rawText);

    Optional<AliasSuggestion> findByIdAndPlantId(Long id, Long plantId);

    List<AliasSuggestion> findByPlantIdAndStatusOrderByOccurrencesDesc(
            Long plantId, AliasSuggestion.SuggestionStatus status);
}
