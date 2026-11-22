package com.benzaazaa.codsettlement.common.web;

import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Only lets clients sort by an explicit allow-list of properties. Sorting on arbitrary
 * paths would let a caller sort by fields they should not reason about (and by joins
 * that are slow).
 */
public final class SortGuard {

    private SortGuard() {
    }

    public static Pageable allow(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException("sort",
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + String.join(", ", allowed.stream().sorted().toList()) + ".");
            }
        }
        return pageable;
    }
}
