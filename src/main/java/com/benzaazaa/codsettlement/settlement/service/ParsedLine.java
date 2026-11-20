package com.benzaazaa.codsettlement.settlement.service;

import java.math.BigDecimal;

/** A validated CSV row: normalized tracking number and scale-2 amounts. */
public record ParsedLine(int rowNumber, String trackingNumber, BigDecimal amountCollected, BigDecimal fee) {
}
