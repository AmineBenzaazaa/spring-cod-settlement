package com.benzaazaa.codsettlement.settlement.web.dto;

import com.benzaazaa.codsettlement.settlement.domain.StatementStatus;
import java.time.LocalDate;

/** Query parameters of {@code GET /api/statements}. {@code merchantId} is a staff filter. */
public record StatementFilter(Long courierId, StatementStatus status, Long merchantId,
                              LocalDate periodFrom, LocalDate periodTo) {

    public static StatementFilter empty() {
        return new StatementFilter(null, null, null, null, null);
    }
}
