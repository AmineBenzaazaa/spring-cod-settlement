package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.config.AsyncConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts processing once the upload transaction has <em>committed</em>, on a background thread.
 *
 * <ul>
 *   <li>{@code AFTER_COMMIT}: the worker never looks for a statement row that is not visible
 *       yet, and nothing is processed if the upload rolls back (Django: {@code on_commit}).</li>
 *   <li>{@code @Async}: the HTTP request returns 202 immediately (Celery: {@code .delay()}).</li>
 * </ul>
 * Both annotations only work through the Spring proxy, which is why this listener is a
 * separate bean from the code that publishes the event.
 */
@Component
public class StatementProcessingListener {

    private final StatementProcessor processor;

    public StatementProcessingListener(StatementProcessor processor) {
        this.processor = processor;
    }

    @Async(AsyncConfig.STATEMENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatementUploaded(StatementUploadedEvent event) {
        processor.process(event.statementId());
    }
}
