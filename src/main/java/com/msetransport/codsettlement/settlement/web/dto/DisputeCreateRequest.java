package com.msetransport.codsettlement.settlement.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** @param claimedAmount defaults to the full discrepancy {@code |collected - expected|} */
public record DisputeCreateRequest(
        @NotNull @Positive Long resultId,
        @NotBlank @Size(max = 2000) String reason,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal claimedAmount) {
}
