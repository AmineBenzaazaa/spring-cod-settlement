package com.msetransport.codsettlement.settlement.service;

/**
 * Published inside the upload transaction. Listeners annotated with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} only receive it once the
 * statement row is committed.
 */
public record StatementUploadedEvent(long statementId) {
}
