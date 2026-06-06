package com.store.ecommerce.util;

import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagingAndSortingHelper {
    private int pageNum = 1;
    private int pageSize = 5;
    private String sortField = "id";
    private String sortDir = "asc";
    private String keyword = null;

    public void setKeyword(String keyword) {
        this.keyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
    }

    public Pageable createPageable() {
        int safePageNum = Math.max(1, this.pageNum);
        int safePageSize = Math.max(1, this.pageSize);

        safePageSize = Math.min(safePageSize, 100);

        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        return PageRequest.of(safePageNum - 1, safePageSize, sort);
    }
}
