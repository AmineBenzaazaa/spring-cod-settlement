package com.msetransport.codsettlement.logistics.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** @param active defaults to {@code true} when omitted */
public record CourierRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) @Pattern(regexp = "[a-z0-9-]+", message = "must be lowercase letters, digits or '-'") String code,
        @Size(max = 30) String contactPhone,
        Boolean active) {

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
