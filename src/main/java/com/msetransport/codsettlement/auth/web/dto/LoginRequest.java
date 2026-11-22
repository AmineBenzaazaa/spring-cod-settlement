package com.msetransport.codsettlement.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 150) String username,
        @NotBlank @Size(max = 200) String password) {
}
