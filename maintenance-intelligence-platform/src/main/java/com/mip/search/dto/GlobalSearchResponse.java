package com.mip.search.dto;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        List<MachineMatch> machines,
        List<RecordMatchResponse> records,
        List<PartMatch> parts,
        List<FailureModeMatch> failureModes
) {
    public record MachineMatch(Long id, String code, String name, String plantName) {
    }

    public record PartMatch(Long id, String partNumber, String name) {
    }

    public record FailureModeMatch(Long id, String code, String name) {
    }
}
