package com.mip.importjob.service;

import com.mip.common.util.TextNormalizer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps the wildly varying column headings and value formats of real maintenance logs
 * onto the canonical record fields. Header matching is normalisation-based, so
 * "Down Time (min)", "downtime_minutes" and "Downtime Mins" all land on downtime.
 */
@Service
public class NormalizationService {

    private static final Map<String, List<String>> HEADER_SYNONYMS = Map.of(
            "machine", List.of("machine", "machine name", "machine id", "equipment", "equipment name",
                    "asset", "asset name", "m c", "m c name", "machine no"),
            "date", List.of("date", "record date", "failure date", "breakdown date", "maintenance date",
                    "reported on", "occurred on", "dt"),
            "downtime", List.of("downtime", "downtime min", "downtime mins", "downtime minutes",
                    "downtime hrs", "downtime hours", "down time", "duration", "duration min",
                    "time lost", "stoppage time"),
            "description", List.of("description", "problem", "problem description", "issue",
                    "failure description", "fault", "complaint", "remarks", "details", "observation"),
            "action", List.of("action", "action taken", "corrective action", "resolution", "fix",
                    "work done", "repair action"),
            "technician", List.of("technician", "engineer", "attended by", "fixed by", "done by",
                    "maintenance engineer", "operator"),
            "failureMode", List.of("failure mode", "failure type", "failure category", "fault type",
                    "breakdown type", "cause", "root cause"),
            "parts", List.of("parts", "part", "spare parts", "parts used", "parts replaced",
                    "spares used", "spare part", "material used"));

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("uuuu/M/d"),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH));

    private static final Pattern HOURS = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:h|hr|hrs|hour|hours)\\b");
    private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*(?:m|min|mins|minute|minutes)?\\b");

    public NormalizedRow normalize(Map<String, String> raw) {
        String machineText = firstValue(raw, "machine");
        String dateText = firstValue(raw, "date");
        String downtimeText = firstValue(raw, "downtime");
        String description = firstValue(raw, "description");
        String actionTaken = firstValue(raw, "action");
        String technician = firstValue(raw, "technician");
        String failureModeText = firstValue(raw, "failureMode");
        String partsText = firstValue(raw, "parts");

        LocalDate date = parseDate(dateText);
        Integer downtime = parseDowntimeMinutes(downtimeText);

        List<String> missing = new ArrayList<>();
        if (machineText == null) {
            missing.add("MISSING_MACHINE");
        }
        if (date == null) {
            missing.add("MISSING_DATE");
        }
        if (downtime == null) {
            missing.add("MISSING_DOWNTIME");
        }
        if (description == null) {
            missing.add("MISSING_DESCRIPTION");
        }

        return new NormalizedRow(machineText, date, downtime, description, actionTaken,
                technician, failureModeText, partsText, missing);
    }

    private String firstValue(Map<String, String> raw, String field) {
        List<String> synonyms = HEADER_SYNONYMS.get(field);
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String header = TextNormalizer.normalize(entry.getKey());
            if (synonyms.contains(header)) {
                String value = entry.getValue() == null ? "" : entry.getValue().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String candidate = text.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(candidate, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    Integer parseDowntimeMinutes(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String candidate = text.trim().toLowerCase(Locale.ROOT);
        Matcher hours = HOURS.matcher(candidate);
        if (hours.find()) {
            double value = Double.parseDouble(hours.group(1).replace(',', '.'));
            return (int) Math.round(value * 60);
        }
        Matcher minutes = MINUTES.matcher(candidate);
        if (minutes.find()) {
            try {
                return Integer.parseInt(minutes.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
