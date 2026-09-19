package com.mip.dictionary.controller;

import com.mip.dictionary.dto.FailureModeResponse;
import com.mip.dictionary.service.FailureModeService;
import com.mip.dictionary.service.SynonymService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final FailureModeService failureModeService;
    private final SynonymService synonymService;

    @GetMapping("/failure-modes")
    public List<FailureModeResponse> listFailureModes() {
        return failureModeService.listFailureModes();
    }

    @GetMapping("/dictionary")
    public Map<String, Map<String, List<String>>> getDictionary() {
        return synonymService.getDictionary();
    }
}
