package com.mip.search.controller;

import com.mip.search.dto.GlobalSearchResponse;
import com.mip.search.dto.RecordMatchResponse;
import com.mip.search.service.SearchService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public GlobalSearchResponse globalSearch(@RequestParam String query,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return searchService.globalSearch(query, principal);
    }

    @GetMapping("/records")
    public List<RecordMatchResponse> searchRecords(@RequestParam String query,
                                                   @AuthenticationPrincipal MipUserDetails principal) {
        return searchService.searchRecords(query, principal);
    }

    @GetMapping("/machines")
    public List<GlobalSearchResponse.MachineMatch> searchMachines(
            @RequestParam String query,
            @AuthenticationPrincipal MipUserDetails principal) {
        return searchService.searchMachines(query, principal);
    }
}
