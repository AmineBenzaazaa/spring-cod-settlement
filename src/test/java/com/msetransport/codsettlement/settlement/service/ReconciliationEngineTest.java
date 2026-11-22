package com.benzaazaa.codsettlement.settlement.service;

import static com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome.AMOUNT_MISMATCH;
import static com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome.MATCHED;
import static com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome.MISSING_FROM_STATEMENT;
import static com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome.UNKNOWN_TRACKING;
import static org.assertj.core.api.Assertions.assertThat;

import com.benzaazaa.codsettlement.logistics.domain.OrderStatus;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure unit tests: no Spring context, no database, each runs in microseconds. */
class ReconciliationEngineTest {

    private static LineData line(String tracking, String collected) {
        return new LineData(null, 1, tracking, new BigDecimal(collected), new BigDecimal("25.00"));
    }

    private static OrderData order(long id, String tracking, String due, OrderStatus status) {
        return new OrderData(id, tracking, new BigDecimal(due), status);
    }

    private static ResultData classifyOne(LineData line, OrderData order) {
        return ReconciliationEngine.classify(List.of(line), Map.of(order.trackingNumber(), order), List.of()).getFirst();
    }

    @Test
    void exactAmountIsMatched() {
        ResultData r = classifyOne(line("T1", "300.00"), order(1, "T1", "300.00", OrderStatus.DELIVERED));

        assertThat(r.outcome()).isEqualTo(MATCHED);
        assertThat(r.difference()).isEqualByComparingTo("0");
        assertThat(r.orderId()).isEqualTo(1L);
    }

    @Test
    void comparisonIgnoresScale() {
        // new BigDecimal("300.0").equals(new BigDecimal("300.00")) is false; compareTo is 0.
        assertThat(classifyOne(line("T1", "300.0"), order(1, "T1", "300.00", OrderStatus.DELIVERED)).outcome())
                .isEqualTo(MATCHED);
    }

    @Test
    void shortPaymentIsAMismatchWithNegativeDifference() {
        ResultData r = classifyOne(line("T1", "250.00"), order(1, "T1", "300.00", OrderStatus.DELIVERED));

        assertThat(r.outcome()).isEqualTo(AMOUNT_MISMATCH);
        assertThat(r.difference()).isEqualTo(new BigDecimal("-50.00"));
    }

    @Test
    void overCollectionIsAlsoAMismatch() {
        ResultData r = classifyOne(line("T1", "310.00"), order(1, "T1", "300.00", OrderStatus.DELIVERED));

        assertThat(r.outcome()).isEqualTo(AMOUNT_MISMATCH);
        assertThat(r.difference()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void unknownTrackingNumber() {
        List<ResultData> results = ReconciliationEngine.classify(List.of(line("NOPE", "99.00")), Map.of(), List.of());

        assertThat(results).singleElement().satisfies(r -> {
            assertThat(r.outcome()).isEqualTo(UNKNOWN_TRACKING);
            assertThat(r.expectedAmount()).isEqualByComparingTo("0");
            assertThat(r.orderId()).isNull();
        });
    }

    @Test
    void returnedParcelExpectsZeroCash() {
        assertThat(classifyOne(line("T1", "0.00"), order(1, "T1", "300.00", OrderStatus.RETURNED)).outcome())
                .isEqualTo(MATCHED);
    }

    @Test
    void cashReportedForAnUndeliveredOrderIsAMismatch() {
        ResultData r = classifyOne(line("T1", "300.00"), order(1, "T1", "300.00", OrderStatus.SHIPPED));

        assertThat(r.outcome()).isEqualTo(AMOUNT_MISMATCH);
        assertThat(r.expectedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void deliveredOrderAbsentFromTheStatementIsMissing() {
        OrderData onStatement = order(1, "T1", "100.00", OrderStatus.DELIVERED);
        OrderData leftOut = order(2, "T2", "200.00", OrderStatus.DELIVERED);

        List<ResultData> results = ReconciliationEngine.classify(
                List.of(line("T1", "100.00")), Map.of("T1", onStatement), List.of(onStatement, leftOut));

        assertThat(results).extracting(ResultData::outcome).containsExactly(MATCHED, MISSING_FROM_STATEMENT);
        assertThat(results.get(1).expectedAmount()).isEqualByComparingTo("200.00");
        assertThat(results.get(1).difference()).isEqualTo(new BigDecimal("-200.00"));
        assertThat(results.get(1).lineId()).isNull();
    }

    @Test
    void emptyInputsGiveNoResults() {
        assertThat(ReconciliationEngine.classify(List.of(), Map.of(), List.of())).isEmpty();
    }

    @Test
    void summaryCountsEveryOutcomeAndTotals() {
        List<LineData> lines = List.of(line("T1", "100.00"), line("T2", "150.00"), line("X", "20.00"));
        Map<String, OrderData> orders = Map.of(
                "T1", order(1, "T1", "100.00", OrderStatus.DELIVERED),
                "T2", order(2, "T2", "200.00", OrderStatus.DELIVERED));
        List<ResultData> results = ReconciliationEngine.classify(lines, orders,
                List.of(order(3, "T3", "50.00", OrderStatus.DELIVERED)));

        ReconciliationSummary summary = ReconciliationEngine.summarize(results, lines);

        assertThat(summary.counts()).containsExactlyInAnyOrderEntriesOf(Map.of(
                MATCHED, 1L, AMOUNT_MISMATCH, 1L, UNKNOWN_TRACKING, 1L, MISSING_FROM_STATEMENT, 1L));
        assertThat(summary.expectedTotal()).isEqualTo(new BigDecimal("350.00"));
        assertThat(summary.collectedTotal()).isEqualTo(new BigDecimal("270.00"));
        assertThat(summary.feesTotal()).isEqualTo(new BigDecimal("75.00"));
        assertThat(summary.netRemitted()).isEqualTo(new BigDecimal("195.00"));
        assertThat(summary.differenceTotal()).isEqualTo(new BigDecimal("-80.00"));
    }

    @Test
    void summaryListsOutcomesWithZeroCounts() {
        ReconciliationSummary summary = ReconciliationEngine.summarize(List.of(), List.of());

        assertThat(summary.counts()).hasSize(ReconciliationOutcome.values().length).containsValue(0L);
        assertThat(summary.netRemitted()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void summaryHasNoRoundingDrift() {
        List<LineData> lines = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            lines.add(new LineData(null, i + 1, "T" + i, new BigDecimal("0.10"), new BigDecimal("0.01")));
        }

        ReconciliationSummary summary = ReconciliationEngine.summarize(List.of(), lines);

        assertThat(summary.collectedTotal()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(summary.feesTotal()).isEqualTo(new BigDecimal("100.00"));
        assertThat(summary.netRemitted()).isEqualTo(new BigDecimal("900.00"));
    }
}
