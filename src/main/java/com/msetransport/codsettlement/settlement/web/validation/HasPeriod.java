package com.msetransport.codsettlement.settlement.web.validation;

import java.time.LocalDate;

/** Anything with a date range that {@link ValidPeriod} can check. */
public interface HasPeriod {

    LocalDate periodStart();

    LocalDate periodEnd();
}
