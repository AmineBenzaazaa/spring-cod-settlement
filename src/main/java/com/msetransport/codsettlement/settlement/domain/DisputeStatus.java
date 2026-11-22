package com.msetransport.codsettlement.settlement.domain;

public enum DisputeStatus {
    OPEN,
    /** The courier accepted and will pay the claimed amount. */
    RESOLVED,
    REJECTED
}
