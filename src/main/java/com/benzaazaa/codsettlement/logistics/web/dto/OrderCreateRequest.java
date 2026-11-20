package com.benzaazaa.codsettlement.logistics.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * @param merchantId required for staff, ignored for merchant users (the merchant always
 *                   comes from the token, never from the body)
 */
public record OrderCreateRequest(
        @Positive Long merchantId,
        @NotNull @Positive Long courierId,
        @NotBlank @Size(max = 64) String trackingNumber,
        @NotBlank @Size(max = 200) String customerName,
        @Size(max = 30) String customerPhone,
        @Size(max = 100) String city,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amountDue) {
}
