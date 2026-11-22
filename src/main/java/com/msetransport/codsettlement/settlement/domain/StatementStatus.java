package com.msetransport.codsettlement.settlement.domain;

public enum StatementStatus {
    /** Stored and queued for asynchronous processing. */
    UPLOADED,
    PROCESSING,
    RECONCILED,
    FAILED
}
