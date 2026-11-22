package com.msetransport.codsettlement.settlement.service;

import com.msetransport.codsettlement.common.money.Money;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The reconciliation rules, as pure functions over records (no Spring, no JPA, no I/O).
 * A statement is always one courier, one merchant, one period.
 *
 * <ul>
 *   <li>Each line is matched to an order by tracking number (unique per courier).</li>
 *   <li>Expected cash = amount due if the order is DELIVERED, else 0 (returned parcels collect nothing).</li>
 *   <li>collected == expected: MATCHED; otherwise AMOUNT_MISMATCH.</li>
 *   <li>No such order: UNKNOWN_TRACKING.</li>
 *   <li>Delivered in the period, not on this statement: MISSING_FROM_STATEMENT.</li>
 * </ul>
 */
public final class ReconciliationEngine {

    private ReconciliationEngine() {
    }

    public static List<ResultData> classify(Collection<LineData> lines, Map<String, OrderData> ordersByTracking,
                                            Collection<OrderData> deliveredInPeriod) {
        List<ResultData> results = new ArrayList<>(lines.size() + deliveredInPeriod.size());
        Set<String> seenTracking = new HashSet<>();

        for (LineData line : lines) {
            seenTracking.add(line.trackingNumber());
            OrderData order = ordersByTracking.get(line.trackingNumber());
            if (order == null) {
                results.add(new ResultData(ReconciliationOutcome.UNKNOWN_TRACKING, line.trackingNumber(),
                        Money.ZERO, line.amountCollected(), line.lineId(), null));
                continue;
            }
            BigDecimal expected = order.expectedCollection();
            // compareTo, not equals: BigDecimal.equals also compares scale, so 10.0 != 10.00.
            ReconciliationOutcome outcome = line.amountCollected().compareTo(expected) == 0
                    ? ReconciliationOutcome.MATCHED
                    : ReconciliationOutcome.AMOUNT_MISMATCH;
            results.add(new ResultData(outcome, line.trackingNumber(), expected, line.amountCollected(),
                    line.lineId(), order.orderId()));
        }

        for (OrderData order : deliveredInPeriod) {
            if (!seenTracking.contains(order.trackingNumber())) {
                results.add(new ResultData(ReconciliationOutcome.MISSING_FROM_STATEMENT, order.trackingNumber(),
                        order.amountDue(), Money.ZERO, null, order.orderId()));
            }
        }
        return results;
    }

    public static ReconciliationSummary summarize(Collection<ResultData> results, Collection<LineData> lines) {
        Map<ReconciliationOutcome, Long> counts = results.stream()
                .collect(Collectors.groupingBy(ResultData::outcome, Collectors.counting()));
        return ReconciliationSummary.of(counts,
                Money.sum(results.stream().map(ResultData::expectedAmount)),
                Money.sum(lines.stream().map(LineData::amountCollected)),
                Money.sum(lines.stream().map(LineData::fee)));
    }

    public static Map<String, OrderData> indexByTracking(Collection<OrderData> orders) {
        return orders.stream().collect(Collectors.toMap(OrderData::trackingNumber, Function.identity()));
    }
}
