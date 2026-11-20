package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.settlement.domain.StatementStatus;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.repository.StatementFileRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imports the CSV lines of a statement and reconciles them. Plain synchronous code; the
 * asynchronous part lives in {@link StatementProcessingListener}, so tests can call this directly.
 *
 * <p>Three short transactions instead of one long one:
 * <ol>
 *   <li>mark PROCESSING (committed, so clients polling the statement see progress);</li>
 *   <li>parse + replace lines + reconcile, all-or-nothing;</li>
 *   <li>on failure, mark FAILED with the reason (in its own transaction, because the
 *       previous one rolled back).</li>
 * </ol>
 */
@Service
public class StatementProcessor {

    private static final Logger log = LoggerFactory.getLogger(StatementProcessor.class);

    private final RemittanceStatementRepository statements;
    private final StatementFileRepository files;
    private final RemittanceCsvParser parser;
    private final StatementIngestionService ingestion;
    private final ReconciliationService reconciliation;
    private final TransactionTemplate tx;

    public StatementProcessor(RemittanceStatementRepository statements, StatementFileRepository files,
                              RemittanceCsvParser parser, StatementIngestionService ingestion,
                              ReconciliationService reconciliation, TransactionTemplate tx) {
        this.statements = statements;
        this.files = files;
        this.parser = parser;
        this.ingestion = ingestion;
        this.reconciliation = reconciliation;
        this.tx = tx;
    }

    public ProcessingResult process(long statementId) {
        Boolean started = tx.execute(status -> statements.findById(statementId)
                .filter(s -> s.getStatus() != StatementStatus.RECONCILED) // duplicate event: nothing to do
                .map(s -> {
                    s.markProcessing();
                    return true;
                })
                .orElse(false));
        if (!Boolean.TRUE.equals(started)) {
            log.info("Statement {} missing or already reconciled; skipping", statementId);
            return ProcessingResult.skipped(statementId);
        }

        try {
            ReconciliationSummary summary = tx.execute(status -> {
                reconciliation.ensureNotDisputed(statementId); // replacing lines would hit disputed results
                byte[] content = files.findById(statementId)
                        .orElseThrow(() -> new IllegalStateException("Stored file is missing"))
                        .getContent();
                List<ParsedLine> parsed = parser.parse(content);
                ingestion.replaceLines(statementId, parsed);
                return reconciliation.reconcile(statementId);
            });
            log.info("Statement {} reconciled: {}", statementId, summary.counts());
            return ProcessingResult.reconciled(statementId, summary);
        } catch (CsvValidationException | ConflictException ex) {
            // Permanent problems: retrying will not help, surface them on the statement.
            markFailed(statementId, ex.getMessage());
            return ProcessingResult.failed(statementId, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Error processing statement {}", statementId, ex);
            String message = "Processing error: " + ex.getMessage();
            markFailed(statementId, message);
            return ProcessingResult.failed(statementId, message);
        }
    }

    private void markFailed(long statementId, String message) {
        tx.executeWithoutResult(status -> statements.findById(statementId).ifPresent(s -> s.markFailed(message)));
    }
}
