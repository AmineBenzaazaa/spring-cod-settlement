package com.benzaazaa.codsettlement.logistics.repository;

import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.logistics.domain.OrderStatus;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable WHERE clauses for orders (the Spring Data equivalent of a django-filter FilterSet).
 * Each method returns {@code null} when its filter is not set, and {@link #matching} only
 * combines the ones that are.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> matching(OrderFilter filter) {
        List<Specification<Order>> specs = new ArrayList<>();
        add(specs, statusIn(filter.status()));
        add(specs, hasCourier(filter.courierId()));
        add(specs, hasMerchant(filter.merchantId()));
        add(specs, inCity(filter.city()));
        add(specs, amountAtLeast(filter.minAmount()));
        add(specs, amountAtMost(filter.maxAmount()));
        add(specs, createdAfter(filter.createdAfter()));
        add(specs, createdBefore(filter.createdBefore()));
        add(specs, search(filter.q()));
        return Specification.allOf(specs);
    }

    public static Specification<Order> hasId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    static Specification<Order> statusIn(Collection<OrderStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null : (root, query, cb) -> root.get("status").in(statuses);
    }

    static Specification<Order> hasCourier(Long courierId) {
        return courierId == null ? null : (root, query, cb) -> cb.equal(root.get("courier").get("id"), courierId);
    }

    static Specification<Order> hasMerchant(Long merchantId) {
        return merchantId == null ? null : (root, query, cb) -> cb.equal(root.get("merchant").get("id"), merchantId);
    }

    static Specification<Order> inCity(String city) {
        return isBlank(city) ? null
                : (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.strip().toLowerCase(Locale.ROOT));
    }

    static Specification<Order> amountAtLeast(BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amountDue"), min);
    }

    static Specification<Order> amountAtMost(BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amountDue"), max);
    }

    static Specification<Order> createdAfter(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<Order> createdBefore(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /** Case-insensitive "contains" across tracking number, customer, phone and city. */
    static Specification<Order> search(String q) {
        if (isBlank(q)) {
            return null;
        }
        String pattern = "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("trackingNumber")), pattern, '\\'),
                cb.like(cb.lower(root.get("customerName")), pattern, '\\'),
                cb.like(cb.lower(root.get("customerPhone")), pattern, '\\'),
                cb.like(cb.lower(root.get("city")), pattern, '\\'));
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void add(List<Specification<Order>> specs, Specification<Order> spec) {
        if (spec != null) {
            specs.add(spec);
        }
    }
}
