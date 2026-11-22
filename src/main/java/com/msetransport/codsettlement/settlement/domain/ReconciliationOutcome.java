package com.msetransport.codsettlement.settlement.domain;

public enum ReconciliationOutcome {
    /** Courier reported exactly the cash we expected. */
    MATCHED,
    /** Courier reported a different amount (short payment, over-collection, cash on a returned parcel). */
    AMOUNT_MISMATCH,
    /** Tracking number on the statement matches none of this merchant's orders with this courier. */
    UNKNOWN_TRACKING,
    /** Order delivered in the period but absent from this statement (and not settled on another). */
    MISSING_FROM_STATEMENT
}
