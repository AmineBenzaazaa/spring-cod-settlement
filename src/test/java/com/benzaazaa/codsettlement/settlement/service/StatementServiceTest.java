package com.benzaazaa.codsettlement.settlement.service;

import static com.benzaazaa.codsettlement.support.TestData.courier;
import static com.benzaazaa.codsettlement.support.TestData.merchant;
import static com.benzaazaa.codsettlement.support.TestData.merchantUser;
import static com.benzaazaa.codsettlement.support.TestData.result;
import static com.benzaazaa.codsettlement.support.TestData.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.domain.StatementStatus;
import com.benzaazaa.codsettlement.settlement.mapper.SettlementMapperImpl;
import com.benzaazaa.codsettlement.settlement.repository.DisputeRepository;
import com.benzaazaa.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.ReconciliationReportResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.ReconciliationResultResponse;
import com.benzaazaa.codsettlement.support.TestData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock RemittanceStatementRepository statements;
    @Mock ReconciliationResultRepository results;
    @Mock DisputeRepository disputes;
    @Mock ReconciliationService reconciliation;
    @Mock ApplicationEventPublisher events;

    private StatementService service;
    private RemittanceStatement statement;

    @BeforeEach
    void setUp() {
        service = new StatementService(statements, results, disputes, reconciliation, new SettlementMapperImpl(), events);
        statement = TestData.statement(10, merchant(1), courier(7));
    }

    @SuppressWarnings("unchecked")
    private void statementIsVisible() {
        when(statements.findOne(any(Specification.class))).thenReturn(Optional.of(statement));
    }

    @Test
    void reportOfAReconciledStatementHasSummaryAndDisputeFlags() {
        statementIsVisible();
        statement.markReconciled(TestData.NOW, 2);
        when(results.findForReport(10L)).thenReturn(List.of(
                result(1, statement, ReconciliationOutcome.MATCHED, "T1", "10.00", "10.00"),
                result(2, statement, ReconciliationOutcome.AMOUNT_MISMATCH, "T2", "10.00", "5.00")));
        when(disputes.findResultIdsByStatementId(10L)).thenReturn(Set.of(2L));
        ReconciliationSummary summary = ReconciliationSummary.of(Map.of(), null, null, null);
        when(reconciliation.summaryFor(10L)).thenReturn(summary);

        ReconciliationReportResponse report = service.report(merchantUser(1), 10L, null);

        assertThat(report.summary()).isSameAs(summary);
        assertThat(report.results()).extracting(ReconciliationResultResponse::hasDispute).containsExactly(false, true);
        assertThat(report.results().get(1).difference()).isEqualByComparingTo("-5.00");
    }

    @Test
    void reportOfAPendingStatementHasNoSummaryAndCanFilterByOutcome() {
        statementIsVisible();
        when(results.findForReport(10L, ReconciliationOutcome.UNKNOWN_TRACKING)).thenReturn(List.of());

        ReconciliationReportResponse report = service.report(merchantUser(1), 10L, ReconciliationOutcome.UNKNOWN_TRACKING);

        assertThat(report.summary()).isNull();
        verify(reconciliation, never()).summaryFor(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void foreignStatementIsNotFound() {
        when(statements.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(merchantUser(2), 10L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reprocessRequeuesAndPublishesTheEvent() {
        statementIsVisible();
        statement.markFailed("boom");

        var response = service.reprocess(staff(), 10L);

        assertThat(response.status()).isEqualTo(StatementStatus.UPLOADED);
        assertThat(response.errorMessage()).isNull();
        verify(events).publishEvent(new StatementUploadedEvent(10L));
    }

    @Test
    void reprocessIsRefusedWhileProcessingOrWhenDisputed() {
        statementIsVisible();
        statement.markProcessing();
        assertThatThrownBy(() -> service.reprocess(staff(), 10L)).hasMessageContaining("already being processed");

        doThrow(new ConflictException("disputed")).when(reconciliation).ensureNotDisputed(10L);
        assertThatThrownBy(() -> service.reprocess(staff(), 10L)).hasMessage("disputed");
        verify(events, never()).publishEvent(any());
    }
}
