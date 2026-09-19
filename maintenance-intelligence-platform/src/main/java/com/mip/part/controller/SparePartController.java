package com.mip.part.controller;

import com.mip.common.dto.PageResponse;
import com.mip.part.dto.PartDetailResponse;
import com.mip.part.dto.PartRowResponse;
import com.mip.part.service.SparePartService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class SparePartController {

    private final SparePartService sparePartService;

    @GetMapping
    public PageResponse<PartRowResponse> listParts(@RequestParam(required = false) String query,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @AuthenticationPrincipal MipUserDetails principal) {
        return sparePartService.listParts(query, page, size, principal);
    }

    @GetMapping("/{partId}")
    public PartDetailResponse getPartDetail(@PathVariable Long partId,
                                            @AuthenticationPrincipal MipUserDetails principal) {
        return sparePartService.getPartDetail(partId, principal);
    }
}
