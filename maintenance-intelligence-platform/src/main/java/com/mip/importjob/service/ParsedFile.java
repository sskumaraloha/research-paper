package com.mip.importjob.service;

import java.util.List;
import java.util.Map;

/** Header row plus data rows (header → cell text) of a parsed import file. */
public record ParsedFile(
        List<String> headers,
        List<Map<String, String>> rows
) {
}
