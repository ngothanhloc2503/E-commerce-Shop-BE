package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> content;

    Integer page;
    Integer size;

    Integer totalPages;
    Long totalItems;

    Boolean first;
    Boolean last;
}
