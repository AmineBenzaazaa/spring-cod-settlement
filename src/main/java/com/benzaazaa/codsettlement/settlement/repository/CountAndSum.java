package com.benzaazaa.codsettlement.settlement.repository;

import java.math.BigDecimal;

/** {@code count(...)} and {@code sum(...)} from one aggregate query; {@code sum} is null for zero rows. */
public record CountAndSum(Long count, BigDecimal sum) {
}
