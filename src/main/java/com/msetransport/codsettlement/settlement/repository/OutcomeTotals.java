package com.msetransport.codsettlement.settlement.repository;

import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;

/** Row of a GROUP BY outcome query (JPQL constructor expression into a record). */
public record OutcomeTotals(ReconciliationOutcome outcome, Long count, BigDecimal expectedTotal) {
}
