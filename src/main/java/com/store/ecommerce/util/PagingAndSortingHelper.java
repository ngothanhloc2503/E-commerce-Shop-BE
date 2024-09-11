package com.store.ecommerce.util;

import com.store.ecommerce.repository.SearchRepository;
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

    public Page<?> getPageEntities(SearchRepository<?, Long> repository) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        Page<?> page = null;

        if (keyword != null && !keyword.isBlank()) {
            page = repository.findAll(keyword, pageable);
        } else {
            page = repository.findAll(pageable);
        }

        return page;
    }
}
