package com.mip.record.dto;

import com.mip.common.dto.NamedRef;

import java.util.List;

public record RecordFilterOptionsResponse(
        List<NamedRef> machines,
        List<NamedRef> lines,
        List<NamedRef> failureModes
) {
}
