package com.msetransport.codsettlement.settlement.web.dto;

import com.msetransport.codsettlement.settlement.web.validation.HasPeriod;
import com.msetransport.codsettlement.settlement.web.validation.ValidPeriod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Form fields sent next to the file in the multipart upload.
 *
 * @param merchantId staff only (required for staff); ignored for merchant users
 */
@ValidPeriod
public record StatementUploadForm(
        @NotNull @Positive Long courierId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @Positive Long merchantId) implements HasPeriod {
}
