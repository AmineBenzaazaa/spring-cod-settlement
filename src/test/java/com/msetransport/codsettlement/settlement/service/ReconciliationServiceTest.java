package com.msetransport.codsettlement.settlement.service;

import static com.msetransport.codsettlement.support.TestData.courier;
import static com.msetransport.codsettlement.support.TestData.delivered;
import static com.msetransport.codsettlement.support.TestData.line;
import static com.msetransport.codsettlement.support.TestData.merchant;
import static com.msetransport.codsettlement.support.TestData.statement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.common.error.ConflictException;
import com.msetransport.codsettlement.common.error.ResourceNotFoundException;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.logistics.domain.Order;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import com.msetransport.codsettlement.settlement.domain.ReconciliationResult;
import com.msetransport.codsettlement.settlement.domain.RemittanceLine;
import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import com.msetransport.codsettlement.settlement.domain.StatementStatus;
import com.msetransport.codsettlement.settlement.repository.DisputeRepository;
import com.msetransport.codsettlement.settlement.repository.MoneyTotals;
import com.msetransport.codsettlement.settlement.repository.OutcomeTotals;
import com.msetransport.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceLineRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.msetransport.codsettlement.settlement.repository.SettlementOrderRepository;
import com.msetransport.codsettlement.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock RemittanceStatementRepository statements;
    @Mock RemittanceLineRepository lines;
    @Mock ReconciliationResultRepository results;
    @Mock SettlementOrderRepository orders;
    @Mock DisputeRepository disputes;
    @Captor ArgumentCaptor<List<ReconciliationResult>> savedResults;

    private ReconciliationService service;
    private final Merchant merchant = merchant(1);
    private final Courier courier = courier(7);
    private RemittanceStatement statement;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(statements, lines, results, orders, disputes,
                Clock.fixed(TestData.NOW, ZoneOffset.UTC), ZoneId.of("Africa/Casablanca"));
        statement = statement(10, merchant, courier);
    }

    @Test
    void classifiesPersistsResultsLinksLinesAndMarksReconciled() {
        Order matched = delivered(1, merchant, courier, "T1", "100.00");
        Order shortPaid = delivered(2, merchant, courier, "T2", "200.00");
        Order missing = delivered(3, merchant, courier, "T3", "300.00");
        RemittanceLine l1 = line(51, statement, 1, "T1", "100.00", "10.00");
        RemittanceLine l2 = line(52, statement, 2, "T2", "150.00", "10.00");
        RemittanceLine l3 = line(53, statement, 3, "NOPE", "20.00", "10.00");
        when(statements.findByIdForUpdate(10L)).thenReturn(Optional.of(statement));
        when(lines.findByStatementIdOrderByRowNumber(10L)).thenReturn(List.of(l1, l2, l3));
        when(orders.findOrdersOnStatement(10L, 1L, 7L)).thenReturn(List.of(matched, shortPaid));
        when(orders.findDeliveredInPeriodNotSettledElsewhere(eq(10L), eq(1L), eq(7L), any(), any()))
                .thenReturn(List.of(matched, shortPaid, missing));

        ReconciliationSummary summary = service.reconcile(10L);

        verify(results).deleteByStatementId(10L);
        verify(results).saveAll(savedResults.capture());
        assertThat(savedResults.getValue())
                .extracting(ReconciliationResult::getOutcome, ReconciliationResult::getTrackingNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(ReconciliationOutcome.MATCHED, "T1"),
                        org.assertj.core.groups.Tuple.tuple(ReconciliationOutcome.AMOUNT_MISMATCH, "T2"),
                        org.assertj.core.groups.Tuple.tuple(ReconciliationOutcome.UNKNOWN_TRACKING, "NOPE"),
                        org.assertj.core.groups.Tuple.tuple(ReconciliationOutcome.MISSING_FROM_STATEMENT, "T3"));
        assertThat(savedResults.getValue().get(1).getLine()).isSameAs(l2);
        assertThat(savedResults.getValue().get(3).getOrder()).isSameAs(missing);

        assertThat(l1.getOrder()).isSameAs(matched);
        assertThat(l2.getOrder()).isSameAs(shortPaid);
        assertThat(l3.getOrder()).isNull();

        assertThat(statement.getStatus()).isEqualTo(StatementStatus.RECONCILED);
        assertThat(statement.getReconciledAt()).isEqualTo(TestData.NOW);
        assertThat(statement.getLineCount()).isEqualTo(3);
        assertThat(summary.collectedTotal()).isEqualTo(new BigDecimal("270.00"));
        assertThat(summary.expectedTotal()).isEqualTo(new BigDecimal("600.00"));
    }

    @Test
    void periodBoundariesUseTheMoroccanBusinessDay() {
        when(statements.findByIdForUpdate(10L)).thenReturn(Optional.of(statement));
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);

        service.reconcile(10L);

        verify(orders).findDeliveredInPeriodNotSettledElsewhere(eq(10L), eq(1L), eq(7L), from.capture(), to.capture());
        // Casablanca is UTC+1: 1 October 00:00 local is 30 September 23:00 UTC.
        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-09-30T23:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-10-31T23:00:00Z"));
    }

    @Test
    void refusesToReplaceResultsThatAreUnderDispute() {
        when(statements.findByIdForUpdate(10L)).thenReturn(Optional.of(statement));
        when(disputes.existsByResultStatementId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.reconcile(10L)).isInstanceOf(ConflictException.class);
        verify(results, never()).deleteByStatementId(anyLong());
    }

    @Test
    void unknownStatementIsNotFound() {
        when(statements.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reconcile(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summaryIsRebuiltFromAggregates() {
        when(results.totalsByOutcome(10L)).thenReturn(List.of(
                new OutcomeTotals(ReconciliationOutcome.MATCHED, 2L, new BigDecimal("300.00")),
                new OutcomeTotals(ReconciliationOutcome.MISSING_FROM_STATEMENT, 1L, new BigDecimal("50.00"))));
        when(lines.totalsForStatement(10L)).thenReturn(new MoneyTotals(new BigDecimal("300.00"), new BigDecimal("20.00")));

        ReconciliationSummary summary = service.summaryFor(10L);

        assertThat(summary.counts().get(ReconciliationOutcome.MATCHED)).isEqualTo(2L);
        assertThat(summary.counts().get(ReconciliationOutcome.UNKNOWN_TRACKING)).isZero();
        assertThat(summary.expectedTotal()).isEqualTo(new BigDecimal("350.00"));
        assertThat(summary.netRemitted()).isEqualTo(new BigDecimal("280.00"));
        assertThat(summary.differenceTotal()).isEqualTo(new BigDecimal("-50.00"));
    }

    @Test
    void summaryOfAnEmptyStatementIsZero() {
        when(results.totalsByOutcome(10L)).thenReturn(List.of());
        when(lines.totalsForStatement(10L)).thenReturn(new MoneyTotals(null, null));

        assertThat(service.summaryFor(10L).collectedTotal()).isEqualTo(new BigDecimal("0.00"));
    }
}
