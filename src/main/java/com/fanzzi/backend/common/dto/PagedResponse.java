package com.fanzzi.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PagedResponse<T> {

    private List<T> content;

    private int page;
    private int size;

    private int totalPages;
    private long totalElements;

    // ✅ REQUIRED (for Redis)
    public PagedResponse() {}

    // ✅ CRITICAL FIX (for Jackson)
    @JsonCreator
    public PagedResponse(
            @JsonProperty("content") List<T> content,
            @JsonProperty("page") int page,
            @JsonProperty("size") int size,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("totalElements") long totalElements
    ) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }

    public static <T> PagedResponse<T> empty(int page, int size) {
        return new PagedResponse<>(
                List.of(),
                page,
                size,
                0,
                0
        );
    }
}