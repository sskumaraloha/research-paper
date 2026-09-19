package com.mip.importjob.service;

import com.mip.exception.FileProcessingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Turns CSV and XLSX maintenance logs into header/row structures. */
@Service
public class FileParserService {

    static final int MAX_ROWS = 5000;

    private final DataFormatter dataFormatter = new DataFormatter();

    public ParsedFile parse(InputStream input, String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".csv")) {
                return parseCsv(input);
            }
            if (lower.endsWith(".xlsx")) {
                return parseXlsx(input);
            }
        } catch (IOException ex) {
            throw new FileProcessingException("Could not read file '" + filename + "': " + ex.getMessage());
        }
        throw new FileProcessingException(
                "Unsupported file type. Upload a .csv or .xlsx maintenance log");
    }

    private ParsedFile parseCsv(InputStream input) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                .build();
        try (CSVParser parser = new CSVParser(
                new InputStreamReader(input, StandardCharsets.UTF_8), format)) {
            List<String> headers = parser.getHeaderNames();
            if (headers.isEmpty()) {
                throw new FileProcessingException("The file has no header row");
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new FileProcessingException(
                            "File exceeds the maximum of " + MAX_ROWS + " rows per import");
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), i < csvRecord.size() ? csvRecord.get(i).trim() : "");
                }
                if (row.values().stream().anyMatch(v -> !v.isBlank())) {
                    rows.add(row);
                }
            }
            return new ParsedFile(headers, rows);
        }
    }

    private ParsedFile parseXlsx(InputStream input) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getRow(sheet.getFirstRowNum()) == null) {
                throw new FileProcessingException("The workbook has no data");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                headers.add(cellText(headerRow.getCell(c)));
            }
            if (headers.stream().allMatch(String::isBlank)) {
                throw new FileProcessingException("The file has no header row");
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    throw new FileProcessingException(
                            "File exceeds the maximum of " + MAX_ROWS + " rows per import");
                }
                Map<String, String> data = new LinkedHashMap<>();
                boolean hasContent = false;
                for (int c = 0; c < headers.size(); c++) {
                    String value = cellText(row.getCell(c));
                    data.put(headers.get(c), value);
                    hasContent = hasContent || !value.isBlank();
                }
                if (hasContent) {
                    rows.add(data);
                }
            }
            return new ParsedFile(headers, rows);
        }
    }

    private String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return dataFormatter.formatCellValue(cell).trim();
    }
}
