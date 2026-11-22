package com.benzaazaa.codsettlement.logistics.domain;

public enum OrderStatus {
    PENDING,
    SHIPPED,
    DELIVERED,
    RETURNED,
    CANCELLED;

    /** Final statuses cannot change any more; delivered/returned orders are part of settlement. */
    public boolean isFinal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }
}
