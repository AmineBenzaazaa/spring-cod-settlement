package com.msetransport.codsettlement.settlement.web.dto;

import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;

/** @param line {@code null} for MISSING_FROM_STATEMENT (there is no line for it) */
public record ReconciliationResultResponse(
        Long id,
        ReconciliationOutcome outcome,
        String trackingNumber,
        Long orderId,
        RemittanceLineResponse line,
        BigDecimal expectedAmount,
        BigDecimal collectedAmount,
        BigDecimal difference,
        boolean hasDispute) {
}
