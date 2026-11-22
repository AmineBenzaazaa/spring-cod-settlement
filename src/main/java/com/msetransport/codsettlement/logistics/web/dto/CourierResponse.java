package com.msetransport.codsettlement.logistics.web.dto;

import java.time.Instant;

public record CourierResponse(Long id, String name, String code, String contactPhone, boolean active, Instant createdAt) {
}
