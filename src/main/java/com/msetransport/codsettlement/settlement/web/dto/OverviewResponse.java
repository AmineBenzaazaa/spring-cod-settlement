package com.msetransport.codsettlement.settlement.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard numbers for one merchant (or all merchants, for staff), in one round trip.
 * Totals use the same formulas as {@link CourierBalanceResponse}, summed over couriers.
 *
 * @param merchantId          {@code null} when staff asked for all merchants
 * @param openDisputes        disputes still waiting for a staff decision
 * @param openDisputedAmount  sum of their claimed amounts
 * @param discrepancies       non-matched results on reconciled statements
 * @param discrepancyAmount   sum of {@code |collected - expected|} over those results
 */
public record OverviewResponse(
        Long merchantId,
        String currency,
        BigDecimal expectedCollections,
        BigDecimal reportedCollections,
        BigDecimal fees,
        BigDecimal netRemitted,
        BigDecimal balanceOwed,
        long openDisputes,
        BigDecimal openDisputedAmount,
        long discrepancies,
        BigDecimal discrepancyAmount,
        List<CourierPosition> couriers) {

    /** One row of the per-courier balance table. */
    public record CourierPosition(
            Long courierId,
            String name,
            String code,
            boolean active,
            long deliveredOrders,
            BigDecimal expectedCollections,
            BigDecimal reportedCollections,
            BigDecimal fees,
            BigDecimal netRemitted,
            BigDecimal balanceOwed) {
    }
}
