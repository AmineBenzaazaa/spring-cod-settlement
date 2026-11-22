package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.common.money.Money;
import com.benzaazaa.codsettlement.logistics.domain.OrderStatus;
import java.math.BigDecimal;

public record OrderData(Long orderId, String trackingNumber, BigDecimal amountDue, OrderStatus status) {

    /** Cash the courier should have collected: the amount due if delivered, otherwise nothing. */
    public BigDecimal expectedCollection() {
        return status == OrderStatus.DELIVERED ? amountDue : Money.ZERO;
    }
}
