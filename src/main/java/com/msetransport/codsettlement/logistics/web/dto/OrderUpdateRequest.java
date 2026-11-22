package com.msetransport.codsettlement.logistics.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Full replacement of the editable fields (PUT). Status and merchant are not editable here. */
public record OrderUpdateRequest(
        @NotNull @Positive Long courierId,
        @NotBlank @Size(max = 64) String trackingNumber,
        @NotBlank @Size(max = 200) String customerName,
        @Size(max = 30) String customerPhone,
        @Size(max = 100) String city,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amountDue) {
}
