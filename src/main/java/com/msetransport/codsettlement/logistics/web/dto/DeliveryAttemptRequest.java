package com.msetransport.codsettlement.logistics.web.dto;

import com.msetransport.codsettlement.logistics.domain.AttemptOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** @param attemptedAt defaults to now when omitted */
public record DeliveryAttemptRequest(
        @NotNull AttemptOutcome outcome,
        @PastOrPresent Instant attemptedAt,
        @Size(max = 255) String note) {
}
