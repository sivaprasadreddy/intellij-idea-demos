package dev.sivalabs.quicknotes.domain.model;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PagedResult<T>(
        List<T> data,
        long totalElements,
        int pageNumber,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious) {

    public PagedResult(Page<T> page) {
        this(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious());
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        List<R> mappedData = data.stream().map(mapper).toList();
        return new PagedResult<>(
                mappedData, totalElements, pageNumber, totalPages, isFirst, isLast, hasNext, hasPrevious);
    }
}
