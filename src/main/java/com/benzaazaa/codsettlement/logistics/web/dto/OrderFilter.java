package com.benzaazaa.codsettlement.logistics.web.dto;

import com.benzaazaa.codsettlement.logistics.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Query parameters of {@code GET /api/orders}, bound by Spring MVC from the query string
 * ({@code ?status=DELIVERED&status=RETURNED&minAmount=300&q=alaoui}).
 *
 * @param merchantId staff only; merchant users are always scoped to their own merchant
 * @param q          free-text search on tracking number, customer name, phone and city
 */
public record OrderFilter(
        List<OrderStatus> status,
        Long courierId,
        Long merchantId,
        String city,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Instant createdAfter,
        Instant createdBefore,
        String q) {

    public static OrderFilter empty() {
        return new OrderFilter(null, null, null, null, null, null, null, null, null);
    }
}
