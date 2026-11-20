package com.benzaazaa.codsettlement.security;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

/**
 * Per-merchant data isolation as a reusable JPA {@link Specification}.
 *
 * <p>Every tenant-owned query is AND-ed with {@code visibleTo(user, ...)}: staff see all
 * rows, a merchant user only rows whose merchant is theirs. Because it is part of the
 * query, a foreign ID behaves exactly like a missing one (404), and lists never include
 * other tenants' rows.
 */
public final class TenantSpecifications {

    private TenantSpecifications() {
    }

    /**
     * @param pathToMerchant attribute path from the entity to its Merchant, e.g. {@code "merchant"}
     *                       or {@code "statement", "merchant"}
     */
    public static <T> Specification<T> visibleTo(AuthenticatedUser user, String... pathToMerchant) {
        if (user.isStaff()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Path<?> path = root;
            for (String attribute : pathToMerchant) {
                path = path.get(attribute);
            }
            return cb.equal(path.get("id"), user.merchantId());
        };
    }
}
