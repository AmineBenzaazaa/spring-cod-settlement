package com.msetransport.codsettlement.logistics.web.dto;

import com.msetransport.codsettlement.logistics.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long merchantId,
        Long courierId,
        String courierName,
        String trackingNumber,
        String customerName,
        String customerPhone,
        String city,
        BigDecimal amountDue,
        String currency,
        OrderStatus status,
        Instant deliveredAt,
        List<DeliveryAttemptResponse> attempts,
        Instant createdAt,
        Instant updatedAt) {
}
