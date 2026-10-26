package com.papererp.common.dto;

import java.util.Collections;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) size);
        return new PageResponse<>(
                content != null ? Collections.unmodifiableList(content) : Collections.emptyList(),
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
}
