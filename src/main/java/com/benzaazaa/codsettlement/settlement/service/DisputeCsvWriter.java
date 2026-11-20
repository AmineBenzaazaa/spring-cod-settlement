package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.settlement.domain.Dispute;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

/** Writes disputes as CSV, e.g. to send to the courier. Safe against CSV/formula injection. */
@Component
public class DisputeCsvWriter {

    static final List<String> HEADER = List.of("dispute_id", "merchant", "courier", "statement_id",
            "tracking_number", "outcome", "expected_amount", "collected_amount", "claimed_amount", "status",
            "reason", "created_at");

    /** Cells starting with these make Excel/Sheets evaluate the cell as a formula. */
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    public String write(List<Dispute> disputes) {
        StringBuilder out = new StringBuilder();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setRecordSeparator("\n").get())) {
            printer.printRecord(HEADER);
            for (Dispute d : disputes) {
                ReconciliationResult r = d.getResult();
                printer.printRecord(
                        d.getId(),
                        safe(d.getMerchant().getName()),
                        safe(r.getStatement().getCourier().getName()),
                        r.getStatement().getId(),
                        safe(r.getTrackingNumber()),
                        r.getOutcome(),
                        money(r.getExpectedAmount()),
                        money(r.getCollectedAmount()),
                        money(d.getClaimedAmount()),
                        d.getStatus(),
                        safe(d.getReason()),
                        d.getCreatedAt());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return out.toString();
    }

    static String safe(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    }

    private static String money(BigDecimal value) {
        return value.toPlainString();
    }
}
