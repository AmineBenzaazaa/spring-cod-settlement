package com.benzaazaa.codsettlement.settlement.web.dto;

import com.benzaazaa.codsettlement.settlement.domain.DisputeDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisputeResolveRequest(@NotNull DisputeDecision decision, @Size(max = 2000) String resolutionNote) {
}
