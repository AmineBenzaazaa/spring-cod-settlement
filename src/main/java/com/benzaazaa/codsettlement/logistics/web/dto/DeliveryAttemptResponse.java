package com.benzaazaa.codsettlement.logistics.web.dto;

import com.benzaazaa.codsettlement.logistics.domain.AttemptOutcome;
import java.time.Instant;

public record DeliveryAttemptResponse(Long id, Instant attemptedAt, AttemptOutcome outcome, String note) {
}
