package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.repository.DisputeRepository;
import com.benzaazaa.codsettlement.settlement.repository.MoneyTotals;
import com.benzaazaa.codsettlement.settlement.repository.OutcomeTotals;
import com.benzaazaa.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceLineRepository;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.repository.SettlementOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a statement's data, runs the pure {@link ReconciliationEngine} and persists the
 * results atomically. Running it twice gives the same results (old results are replaced),
 * which makes retries and manual re-runs safe.
 */
@Service
public class ReconciliationService {

    private final RemittanceStatementRepository statements;
    private final RemittanceLineRepository lines;
    private final ReconciliationResultRepository results;
    private final SettlementOrderRepository orders;
    private final DisputeRepository disputes;
    private final Clock clock;
    private final ZoneId businessZone;

    public ReconciliationService(RemittanceStatementRepository statements, RemittanceLineRepository lines,
                                 ReconciliationResultRepository results, SettlementOrderRepository orders,
                                 DisputeRepository disputes, Clock clock, @Value("${app.settlement.zone:Africa/Casablanca}") ZoneId businessZone) {
        this.statements = statements;
        this.lines = lines;
        this.results = results;
        this.orders = orders;
        this.disputes = disputes;
        this.clock = clock;
        this.businessZone = businessZone;
    }

    @Transactional
    public ReconciliationSummary reconcile(long statementId) {
        // SELECT ... FOR UPDATE: a second run for the same statement waits here.
        RemittanceStatement statement = statements.findByIdForUpdate(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement", statementId));
        ensureNotDisputed(statementId);
        Long merchantId = statement.getMerchant().getId();
        Long courierId = statement.getCourier().getId();

        List<RemittanceLine> lineRows = lines.findByStatementIdOrderByRowNumber(statementId);
        List<LineData> lineData = lineRows.stream()
                .map(l -> new LineData(l.getId(), l.getRowNumber(), l.getTrackingNumber(), l.getAmountCollected(), l.getFee()))
                .toList();

        // Two queries for everything the rules need: no per-line lookups.
        List<Order> onStatement = orders.findOrdersOnStatement(statementId, merchantId, courierId);
        List<Order> deliveredInPeriod = orders.findDeliveredInPeriodNotSettledElsewhere(
                statementId, merchantId, courierId, startOf(statement), endOf(statement));

        List<ResultData> classified = ReconciliationEngine.classify(
                lineData,
                ReconciliationEngine.indexByTracking(onStatement.stream().map(ReconciliationService::toData).toList()),
                deliveredInPeriod.stream().map(ReconciliationService::toData).toList());

        Map<Long, RemittanceLine> lineById = lineRows.stream()
                .collect(Collectors.toMap(RemittanceLine::getId, Function.identity()));
        Map<Long, Order> orderById = new HashMap<>();
        onStatement.forEach(o -> orderById.put(o.getId(), o));
        deliveredInPeriod.forEach(o -> orderById.put(o.getId(), o));

        results.deleteByStatementId(statementId);
        results.saveAll(classified.stream()
                .map(r -> new ReconciliationResult(statement, r.outcome(),
                        r.lineId() == null ? null : lineById.get(r.lineId()),
                        r.orderId() == null ? null : orderById.get(r.orderId()),
                        r.trackingNumber(), r.expectedAmount(), r.collectedAmount(), r.difference()))
                .toList());

        // Link lines to their orders so courier balances are plain joins later.
        // Dirty checking turns these setter calls into batched UPDATEs at commit.
        Map<String, Order> orderByTracking = onStatement.stream()
                .collect(Collectors.toMap(Order::getTrackingNumber, Function.identity()));
        lineRows.forEach(l -> l.linkToOrder(orderByTracking.get(l.getTrackingNumber())));

        statement.markReconciled(clock.instant(), lineRows.size());
        return ReconciliationEngine.summarize(classified, lineData);
    }

    /** Re-running would replace results that are under dispute, destroying the evidence. */
    @Transactional(readOnly = true)
    public void ensureNotDisputed(long statementId) {
        if (disputes.existsByResultStatementId(statementId)) {
            throw new ConflictException("Statement has disputes; it can no longer be re-processed.");
        }
    }

    /** Rebuilds the summary from persisted rows with two aggregate queries (used by the report endpoint). */
    @Transactional(readOnly = true)
    public ReconciliationSummary summaryFor(long statementId) {
        List<OutcomeTotals> byOutcome = results.totalsByOutcome(statementId);
        MoneyTotals lineTotals = lines.totalsForStatement(statementId);
        return ReconciliationSummary.of(
                byOutcome.stream().collect(Collectors.toMap(OutcomeTotals::outcome, OutcomeTotals::count)),
                byOutcome.stream().map(OutcomeTotals::expectedTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                lineTotals == null ? null : lineTotals.first(),
                lineTotals == null ? null : lineTotals.second());
    }

    /** Period dates are business days in Morocco, so boundaries are computed in that zone. */
    private Instant startOf(RemittanceStatement statement) {
        return statement.getPeriodStart().atStartOfDay(businessZone).toInstant();
    }

    private Instant endOf(RemittanceStatement statement) {
        return statement.getPeriodEnd().plusDays(1).atStartOfDay(businessZone).toInstant();
    }

    private static OrderData toData(Order order) {
        return new OrderData(order.getId(), order.getTrackingNumber(), order.getAmountDue(), order.getStatus());
    }
}
