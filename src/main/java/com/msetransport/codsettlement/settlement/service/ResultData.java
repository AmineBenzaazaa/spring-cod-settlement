package com.msetransport.codsettlement.settlement.service;

import com.msetransport.codsettlement.common.money.Money;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;

public record ResultData(ReconciliationOutcome outcome, String trackingNumber, BigDecimal expectedAmount,
                         BigDecimal collectedAmount, Long lineId, Long orderId) {

    /** collected - expected (negative = shortfall). */
    public BigDecimal difference() {
        return Money.normalize(collectedAmount.subtract(expectedAmount));
    }
}
