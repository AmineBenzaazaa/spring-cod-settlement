package com.benzaazaa.codsettlement.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paginated lists. Serializing Spring's {@code PageImpl} directly is
 * discouraged (its JSON is an implementation detail), so we expose our own record.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
