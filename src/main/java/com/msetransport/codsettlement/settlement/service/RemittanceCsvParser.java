package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.common.money.InvalidAmountException;
import com.benzaazaa.codsettlement.common.money.Money;
import com.benzaazaa.codsettlement.logistics.domain.TrackingNumbers;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

/**
 * Parses and validates courier remittance CSV files.
 *
 * <pre>
 * tracking_number,amount_collected,fee
 * SPX-000123,249.00,25.00
 * </pre>
 *
 * Header row required; column order is free, header case/spaces are tolerated and extra
 * columns are ignored. Validation is all-or-nothing: a file with any bad row is rejected
 * and every problem is reported, so the courier can fix the file in one go.
 */
@Component
public class RemittanceCsvParser {

    public static final List<String> REQUIRED_COLUMNS = List.of("tracking_number", "amount_collected", "fee");
    static final int MAX_ROWS = 50_000;
    static final int MAX_REPORTED_ERRORS = 50;

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()                 // read the header from the first record
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setIgnoreSurroundingSpaces(true)
            .setAllowMissingColumnNames(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
            .get();

    public List<ParsedLine> parse(byte[] content) {
        String text = decodeUtf8(content);
        if (text.startsWith("﻿")) {
            text = text.substring(1); // BOM added by Excel's "CSV UTF-8" export
        }

        List<String> header;
        List<CSVRecord> records;
        try (CSVParser parser = FORMAT.parse(new StringReader(text))) {
            header = parser.getHeaderNames();
            records = parser.getRecords();
        } catch (IOException | UncheckedIOException | IllegalStateException ex) {
            throw new CsvValidationException("File is not a valid CSV: " + rootMessage(ex));
        }
        if (header.isEmpty()) {
            throw new CsvValidationException("File is empty.");
        }

        Map<String, Integer> columns = indexColumns(header);
        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !columns.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new CsvValidationException("Missing required column(s): " + String.join(", ", missing) + ".");
        }
        if (records.size() > MAX_ROWS) {
            throw new CsvValidationException("File has more than " + MAX_ROWS + " rows.");
        }

        List<ParsedLine> lines = new ArrayList<>(records.size());
        List<String> errors = new ArrayList<>();
        Map<String, Integer> firstSeenRow = new HashMap<>();
        int rowNumber = 0;

        for (CSVRecord record : records) {
            if (isBlank(record)) {
                continue; // a row of empty cells (",,"), common at the end of exported files
            }
            rowNumber++;
            List<String> rowErrors = new ArrayList<>();

            String tracking = TrackingNumbers.normalize(value(record, columns.get("tracking_number")));
            if (tracking.isEmpty()) {
                rowErrors.add("tracking_number is empty");
            } else if (tracking.length() > 64) {
                rowErrors.add("tracking_number is longer than 64 characters");
            } else if (firstSeenRow.containsKey(tracking)) {
                rowErrors.add("duplicate tracking_number " + tracking + " (first seen row " + firstSeenRow.get(tracking) + ")");
            }
            BigDecimal collected = amount(record, columns.get("amount_collected"), "amount_collected", rowErrors);
            BigDecimal fee = amount(record, columns.get("fee"), "fee", rowErrors);

            if (!rowErrors.isEmpty()) {
                for (String e : rowErrors) {
                    errors.add("Row " + rowNumber + ": " + e);
                }
                if (errors.size() >= MAX_REPORTED_ERRORS) {
                    break;
                }
                continue;
            }
            firstSeenRow.put(tracking, rowNumber);
            lines.add(new ParsedLine(rowNumber, tracking, collected, fee));
        }

        if (!errors.isEmpty()) {
            throw new CsvValidationException(errors.subList(0, Math.min(errors.size(), MAX_REPORTED_ERRORS)));
        }
        if (lines.isEmpty()) {
            throw new CsvValidationException("File contains no data rows.");
        }
        return lines;
    }

    /** Hex SHA-256 of the raw bytes: the idempotency key of an upload. */
    public static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", ex);
        }
    }

    private static String decodeUtf8(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw new CsvValidationException("File must be UTF-8 encoded.");
        }
    }

    /** "Tracking Number " becomes "tracking_number". The first occurrence of a column wins. */
    private static Map<String, Integer> indexColumns(List<String> header) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i) == null ? "" : header.get(i).strip().toLowerCase(Locale.ROOT).replace(' ', '_');
            if (!name.isEmpty()) {
                columns.putIfAbsent(name, i);
            }
        }
        return columns;
    }

    private static BigDecimal amount(CSVRecord record, int column, String name, List<String> rowErrors) {
        try {
            return Money.parse(value(record, column));
        } catch (InvalidAmountException ex) {
            rowErrors.add(name + ": " + ex.getMessage());
            return null;
        }
    }

    private static String value(CSVRecord record, int column) {
        return column < record.size() ? record.get(column) : "";
    }

    private static boolean isBlank(CSVRecord record) {
        for (String v : record) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
