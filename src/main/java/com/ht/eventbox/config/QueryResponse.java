package com.ht.eventbox.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryResponse<T> extends Response<List<T>> {
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;
    private int numberOfElements;

    public QueryResponse(int code, String message, List<T> data, int totalPages, long totalElements, int size, int number, int numberOfElements) {
        super(code, message, data);
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.size = size;
        this.number = number;
        this.numberOfElements = numberOfElements;
    }

    public static <T> QueryResponse<T> from(Page<T> page, int code, String message) {
        return new QueryResponse<>(
                code,
                message,
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.getNumberOfElements()
        );
    }
}
