package com.benzaazaa.codsettlement.settlement.web.dto;

import java.math.BigDecimal;

public record RemittanceLineResponse(int rowNumber, String trackingNumber, BigDecimal amountCollected, BigDecimal fee) {
}
