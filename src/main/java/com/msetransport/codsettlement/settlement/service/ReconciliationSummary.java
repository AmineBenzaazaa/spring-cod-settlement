package com.msetransport.codsettlement.settlement.service;

import com.msetransport.codsettlement.common.money.Money;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Totals of a reconciled statement.
 *
 * @param netRemitted     collected - fees: what actually reached the merchant's bank
 * @param differenceTotal collected - expected
 */
public record ReconciliationSummary(
        Map<ReconciliationOutcome, Long> counts,
        BigDecimal expectedTotal,
        BigDecimal collectedTotal,
        BigDecimal feesTotal,
        BigDecimal netRemitted,
        BigDecimal differenceTotal) {

    public static ReconciliationSummary of(Map<ReconciliationOutcome, Long> counts, BigDecimal expected,
                                           BigDecimal collected, BigDecimal fees) {
        Map<ReconciliationOutcome, Long> all = new EnumMap<>(ReconciliationOutcome.class);
        for (ReconciliationOutcome outcome : ReconciliationOutcome.values()) {
            all.put(outcome, counts.getOrDefault(outcome, 0L));
        }
        BigDecimal e = Money.normalize(expected);
        BigDecimal c = Money.normalize(collected);
        BigDecimal f = Money.normalize(fees);
        return new ReconciliationSummary(Map.copyOf(all), e, c, f,
                Money.normalize(c.subtract(f)), Money.normalize(c.subtract(e)));
    }
}
