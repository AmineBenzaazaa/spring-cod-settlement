package com.msetransport.codsettlement.logistics.domain;

/** What happened at the door, and which order status it leads to. */
public enum AttemptOutcome {
    DELIVERED(OrderStatus.DELIVERED),
    NO_ANSWER(OrderStatus.SHIPPED),
    REFUSED(OrderStatus.RETURNED),
    RESCHEDULED(OrderStatus.SHIPPED),
    RETURNED(OrderStatus.RETURNED);

    private final OrderStatus resultingStatus;

    AttemptOutcome(OrderStatus resultingStatus) {
        this.resultingStatus = resultingStatus;
    }

    public OrderStatus resultingStatus() {
        return resultingStatus;
    }
}
