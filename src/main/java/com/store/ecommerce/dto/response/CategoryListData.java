package com.store.ecommerce.dto.response;

import com.store.ecommerce.dto.CategoryDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListData {
    private List<CategoryDTO> categories;
}