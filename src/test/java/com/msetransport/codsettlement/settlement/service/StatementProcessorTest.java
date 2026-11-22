package com.benzaazaa.codsettlement.settlement.service;

import static com.benzaazaa.codsettlement.support.TestData.courier;
import static com.benzaazaa.codsettlement.support.TestData.merchant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.domain.StatementFile;
import com.benzaazaa.codsettlement.settlement.domain.StatementStatus;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.repository.StatementFileRepository;
import com.benzaazaa.codsettlement.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class StatementProcessorTest {

    private static final byte[] CSV = "tracking_number,amount_collected,fee\nT1,10.00,1.00\n".getBytes(StandardCharsets.UTF_8);

    @Mock RemittanceStatementRepository statements;
    @Mock StatementFileRepository files;
    @Mock StatementIngestionService ingestion;
    @Mock ReconciliationService reconciliation;

    private StatementProcessor processor;
    private RemittanceStatement statement;

    @BeforeEach
    void setUp() {
        processor = new StatementProcessor(statements, files, new RemittanceCsvParser(), ingestion, reconciliation,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        statement = TestData.statement(42, merchant(1), courier(7));
    }

    @Test
    void importsLinesAndReconciles() {
        when(statements.findById(42L)).thenReturn(Optional.of(statement));
        when(files.findById(42L)).thenReturn(Optional.of(new StatementFile(statement, CSV)));
        ReconciliationSummary summary = ReconciliationSummary.of(Map.of(), null, null, null);
        when(reconciliation.reconcile(42L)).thenReturn(summary);

        ProcessingResult result = processor.process(42L);

        assertThat(result.outcome()).isEqualTo(ProcessingResult.Outcome.RECONCILED);
        assertThat(result.summary()).isSameAs(summary);
        verify(reconciliation).ensureNotDisputed(42L);
        verify(ingestion).replaceLines(42L, List.of(new ParsedLine(1, "T1", TestData.money("10.00"), TestData.money("1.00"))));
        assertThat(statement.getStatus()).isEqualTo(StatementStatus.PROCESSING); // set to RECONCILED by the (mocked) service
    }

    @Test
    void alreadyReconciledStatementIsSkipped() {
        statement.markReconciled(TestData.NOW, 1);
        when(statements.findById(42L)).thenReturn(Optional.of(statement));

        assertThat(processor.process(42L).outcome()).isEqualTo(ProcessingResult.Outcome.SKIPPED);
        verify(reconciliation, never()).reconcile(anyLong());
    }

    @Test
    void missingStatementIsSkipped() {
        when(statements.findById(42L)).thenReturn(Optional.empty());

        assertThat(processor.process(42L).outcome()).isEqualTo(ProcessingResult.Outcome.SKIPPED);
    }

    @Test
    void corruptStoredFileMarksTheStatementFailed() {
        when(statements.findById(42L)).thenReturn(Optional.of(statement));
        when(files.findById(42L)).thenReturn(Optional.of(new StatementFile(statement, "garbage".getBytes(StandardCharsets.UTF_8))));

        ProcessingResult result = processor.process(42L);

        assertThat(result.outcome()).isEqualTo(ProcessingResult.Outcome.FAILED);
        assertThat(statement.getStatus()).isEqualTo(StatementStatus.FAILED);
        assertThat(statement.getErrorMessage()).startsWith("Missing required column(s)");
        verify(ingestion, never()).replaceLines(anyLong(), anyList());
    }

    @Test
    void disputedStatementIsNotReprocessed() {
        when(statements.findById(42L)).thenReturn(Optional.of(statement));
        doThrow(new ConflictException("Statement has disputes; it can no longer be re-processed."))
                .when(reconciliation).ensureNotDisputed(42L);

        ProcessingResult result = processor.process(42L);

        assertThat(result.outcome()).isEqualTo(ProcessingResult.Outcome.FAILED);
        assertThat(statement.getErrorMessage()).contains("has disputes");
    }

    @Test
    void unexpectedErrorsAreRecordedOnTheStatement() {
        when(statements.findById(42L)).thenReturn(Optional.of(statement));
        when(files.findById(42L)).thenReturn(Optional.of(new StatementFile(statement, CSV)));
        when(reconciliation.reconcile(42L)).thenThrow(new IllegalStateException("database went away"));

        ProcessingResult result = processor.process(42L);

        assertThat(result.outcome()).isEqualTo(ProcessingResult.Outcome.FAILED);
        assertThat(statement.getErrorMessage()).isEqualTo("Processing error: database went away");
    }
}
