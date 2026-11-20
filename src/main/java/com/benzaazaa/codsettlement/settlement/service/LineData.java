package com.benzaazaa.codsettlement.settlement.service;

import java.math.BigDecimal;

/** Plain input to {@link ReconciliationEngine}: no JPA, so the rules are unit-testable in microseconds. */
public record LineData(Long lineId, int rowNumber, String trackingNumber, BigDecimal amountCollected, BigDecimal fee) {
}
