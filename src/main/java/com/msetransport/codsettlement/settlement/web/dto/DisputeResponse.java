package com.msetransport.codsettlement.settlement.web.dto;

import com.msetransport.codsettlement.settlement.domain.DisputeStatus;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import java.math.BigDecimal;
import java.time.Instant;

public record DisputeResponse(
        Long id,
        Long resultId,
        Long statementId,
        String trackingNumber,
        ReconciliationOutcome outcome,
        Long merchantId,
        String reason,
        BigDecimal claimedAmount,
        DisputeStatus status,
        String resolutionNote,
        Instant createdAt,
        Instant resolvedAt) {
}
