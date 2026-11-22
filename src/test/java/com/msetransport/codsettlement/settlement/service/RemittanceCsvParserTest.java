package com.benzaazaa.codsettlement.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class RemittanceCsvParserTest {

    private final RemittanceCsvParser parser = new RemittanceCsvParser();

    private List<ParsedLine> parse(String csv) {
        return parser.parse(csv.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> errorsOf(Runnable action) {
        try {
            action.run();
        } catch (CsvValidationException ex) {
            return ex.getMessages();
        }
        throw new AssertionError("expected CsvValidationException");
    }

    @Test
    void parsesAValidFile() {
        List<ParsedLine> lines = parse("""
                tracking_number,amount_collected,fee
                SPX-1,249.00,25.00
                SPX-2,0,10
                """);

        assertThat(lines).containsExactly(
                new ParsedLine(1, "SPX-1", new BigDecimal("249.00"), new BigDecimal("25.00")),
                new ParsedLine(2, "SPX-2", new BigDecimal("0.00"), new BigDecimal("10.00")));
    }

    @Test
    void headerIsFlexibleBomIsStrippedAndExtraColumnsAreIgnored() {
        List<ParsedLine> lines = parse("﻿ Fee ,Notes,Tracking Number,AMOUNT_COLLECTED\n25,hello, spx-9 ,100.5\n");

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.trackingNumber()).isEqualTo("SPX-9"); // trimmed and upper-cased
            assertThat(line.amountCollected()).isEqualTo(new BigDecimal("100.50"));
            assertThat(line.fee()).isEqualTo(new BigDecimal("25.00"));
        });
    }

    @Test
    void blankLinesAndEmptyRowsAreIgnored() {
        List<ParsedLine> lines = parse("tracking_number,amount_collected,fee\n\nSPX-1,1.00,0\n,,\n\nSPX-2,2.00,0\n");

        assertThat(lines).extracting(ParsedLine::rowNumber).containsExactly(1, 2);
    }

    @Test
    void quotedFieldsAreSupported() {
        List<ParsedLine> lines = parse("tracking_number,amount_collected,fee\n\"SPX-1\",\"1 000.00\",\"0\"\n");

        assertThat(lines.getFirst().amountCollected()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void emptyFileIsRejected() {
        assertThat(errorsOf(() -> parse(""))).containsExactly("File is empty.");
    }

    @Test
    void missingColumnsAreReported() {
        assertThat(errorsOf(() -> parse("tracking_number,amount\nSPX-1,1\n")))
                .containsExactly("Missing required column(s): amount_collected, fee.");
    }

    @Test
    void headerOnlyFileHasNoDataRows() {
        assertThat(errorsOf(() -> parse("tracking_number,amount_collected,fee\n")))
                .containsExactly("File contains no data rows.");
    }

    @Test
    void everyBadRowIsReportedAtOnce() {
        List<String> errors = errorsOf(() -> parse("""
                tracking_number,amount_collected,fee
                ,10.00,1.00
                SPX-1,abc,1.00
                SPX-2,10.005,-1
                SPX-3,10.00,1.00
                spx-3,10.00,1.00
                """));

        assertThat(errors).containsExactly(
                "Row 1: tracking_number is empty",
                "Row 2: amount_collected: 'abc' is not a number",
                "Row 3: amount_collected: '10.005' has more than 2 decimal places",
                "Row 3: fee: '-1' must not be negative",
                "Row 5: duplicate tracking_number SPX-3 (first seen row 4)");
    }

    @Test
    void reportedErrorsAreCapped() {
        StringBuilder csv = new StringBuilder("tracking_number,amount_collected,fee\n");
        for (int i = 0; i < 200; i++) {
            csv.append("SPX-").append(i).append(",bad,1\n");
        }

        assertThat(errorsOf(() -> parse(csv.toString()))).hasSize(RemittanceCsvParser.MAX_REPORTED_ERRORS);
    }

    @Test
    void rowLimitIsEnforced() {
        StringBuilder csv = new StringBuilder("tracking_number,amount_collected,fee\n");
        for (int i = 0; i <= RemittanceCsvParser.MAX_ROWS; i++) {
            csv.append("T").append(i).append(",1,0\n");
        }

        assertThat(errorsOf(() -> parse(csv.toString()))).containsExactly("File has more than 50000 rows.");
    }

    @Test
    void trackingNumbersLongerThanTheColumnAreRejected() {
        assertThat(errorsOf(() -> parse("tracking_number,amount_collected,fee\n" + "X".repeat(65) + ",1,0\n")))
                .containsExactly("Row 1: tracking_number is longer than 64 characters");
    }

    @Test
    void nonUtf8ContentIsRejected() {
        byte[] latin1 = "tracking_number,amount_collected,fee\nCAFÉ,1,0\n".getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> parser.parse(latin1)).hasMessage("File must be UTF-8 encoded.");
    }

    @Test
    void malformedCsvIsRejected() {
        assertThatThrownBy(() -> parse("tracking_number,amount_collected,fee\n\"SPX-1,1,0\n"))
                .isInstanceOf(CsvValidationException.class)
                .hasMessageStartingWith("File is not a valid CSV");
    }

    @Test
    void sha256DependsOnlyOnContent() {
        byte[] a = "same".getBytes(StandardCharsets.UTF_8);

        assertThat(RemittanceCsvParser.sha256Hex(a)).isEqualTo(RemittanceCsvParser.sha256Hex(a.clone()));
        assertThat(RemittanceCsvParser.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
