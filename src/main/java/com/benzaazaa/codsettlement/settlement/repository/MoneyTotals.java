package com.benzaazaa.codsettlement.settlement.repository;

import java.math.BigDecimal;

/** Two sums from one aggregate query. SUM over zero rows is NULL in SQL, so either can be null. */
public record MoneyTotals(BigDecimal first, BigDecimal second) {
}
