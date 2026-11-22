package com.msetransport.codsettlement.settlement.web.dto;

import com.msetransport.codsettlement.settlement.domain.StatementStatus;
import java.time.Instant;
import java.time.LocalDate;

public record StatementResponse(
        Long id,
        Long merchantId,
        Long courierId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String originalFilename,
        String fileSha256,
        String currency,
        StatementStatus status,
        String errorMessage,
        int lineCount,
        Instant uploadedAt,
        Instant reconciledAt) {
}
