package com.benzaazaa.codsettlement.settlement.service;

/**
 * Published inside the upload transaction. Listeners annotated with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} only receive it once the
 * statement row is committed (the Spring equivalent of Django's {@code transaction.on_commit}).
 */
public record StatementUploadedEvent(long statementId) {
}
