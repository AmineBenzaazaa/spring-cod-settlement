package com.msetransport.codsettlement.settlement.domain;

/** What staff can decide when closing a dispute. A separate type makes "resolve to OPEN" unrepresentable. */
public enum DisputeDecision {
    RESOLVED(DisputeStatus.RESOLVED),
    REJECTED(DisputeStatus.REJECTED);

    private final DisputeStatus status;

    DisputeDecision(DisputeStatus status) {
        this.status = status;
    }

    public DisputeStatus toStatus() {
        return status;
    }
}
