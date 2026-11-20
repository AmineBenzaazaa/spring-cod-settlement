package com.benzaazaa.codsettlement.settlement.web.dto;

import java.math.BigDecimal;

/**
 * Courier cash position.
 *
 * <pre>
 * expectedCollections = sum of amountDue of DELIVERED orders
 * reportedCollections = sum of amountCollected on reconciled lines linked to the courier's orders
 * fees                = sum of fee on those lines
 * netRemitted         = reportedCollections - fees          (what reached the bank)
 * balanceOwed         = expectedCollections - reportedCollections
 * </pre>
 *
 * @param merchantId {@code null} when staff asked for all merchants
 */
public record CourierBalanceResponse(
        Long courierId,
        Long merchantId,
        String currency,
        long deliveredOrders,
        BigDecimal expectedCollections,
        BigDecimal reportedCollections,
        BigDecimal fees,
        BigDecimal netRemitted,
        BigDecimal balanceOwed) {
}
