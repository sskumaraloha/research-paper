package com.mip.dictionary.service;

import com.mip.dictionary.dto.FailureModeResponse;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.entity.SynonymDomain;
import com.mip.dictionary.repository.FailureModeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FailureModeService {

    private final FailureModeRepository failureModeRepository;
    private final SynonymService synonymService;

    @Transactional(readOnly = true)
    public List<FailureModeResponse> listFailureModes() {
        return failureModeRepository.findAllByOrderByNameAsc().stream()
                .map(fm -> new FailureModeResponse(fm.getId(), fm.getCode(), fm.getName(),
                        fm.getCategory().name(), fm.keywordList()))
                .toList();
    }

    /**
     * Resolves free text to a failure mode: exact code/name match first, then the
     * synonym dictionary's canonical form, then keyword containment.
     */
    @Transactional(readOnly = true)
    public Optional<FailureMode> resolveFailureMode(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String needle = text.trim().toLowerCase(Locale.ROOT);

        Optional<FailureMode> direct = failureModeRepository.findByCodeIgnoreCase(needle)
                .or(() -> failureModeRepository.findByNameIgnoreCase(needle));
        if (direct.isPresent()) {
            return direct;
        }

        String canonical = synonymService.canonicalise(needle, SynonymDomain.FAILURE_MODE);
        if (!canonical.equals(needle)) {
            Optional<FailureMode> viaSynonym = failureModeRepository.findByNameIgnoreCase(canonical)
                    .or(() -> failureModeRepository.findByCodeIgnoreCase(canonical));
            if (viaSynonym.isPresent()) {
                return viaSynonym;
            }
        }

        return keywordMatch(needle).or(() -> canonical.equals(needle)
                ? Optional.empty()
                : keywordMatch(canonical));
    }

    private Optional<FailureMode> keywordMatch(String needle) {
        FailureMode best = null;
        int bestLength = 0;
        for (FailureMode fm : failureModeRepository.findAll()) {
            if (needle.contains(fm.getName().toLowerCase(Locale.ROOT))
                    && fm.getName().length() > bestLength) {
                best = fm;
                bestLength = fm.getName().length();
                continue;
            }
            for (String keyword : fm.keywordList()) {
                if (needle.contains(keyword) && keyword.length() > bestLength) {
                    best = fm;
                    bestLength = keyword.length();
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
