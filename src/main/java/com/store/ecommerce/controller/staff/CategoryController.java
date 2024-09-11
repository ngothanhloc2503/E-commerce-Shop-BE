package com.store.ecommerce.controller.staff;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.CategoryMapper;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.CategoryService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController("ManageCategoryController")
@RequestMapping("/api/staff/categories")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private AWSS3Service awsS3Service;

    @GetMapping("")
    public ResponseEntity<?> getCategoryByPage(PagingAndSortingHelper helper) {
        if (helper.getPageSize() < 1) {
            List<CategoryDTO> categories = categoryService.getAllCategories(helper.getKeyword(), helper.getSortField(), helper.getSortDir());
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(categories)
                    .totalPages(1)
                    .totalItems((long) categories.size()).build());
        } else {
            Page<CategoryDTO> page = categoryService.getCategoriesByPage(helper);

            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalItems(page.getTotalElements()).build());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(id));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/check-name-unique")
    public boolean checkUniqueName(@RequestParam("id") Long id,
                                   @RequestParam("name") String name) {
        return categoryService.isNameUnique(id, name);
    }

    @PostMapping(path = "/save", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> saveCategory(@RequestPart("category") CategoryDTO categoryDTO,
                                          @RequestPart(name = "image", required = false) MultipartFile image) throws IOException {
        if (!isFileNullAndEmpty(image)) {
            String imageName = StringUtils.cleanPath(image.getOriginalFilename());
            categoryDTO.setImage(imageName);

            CategoryDTO savedCategory = null;
            try {
                savedCategory = categoryService.save(categoryDTO);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
            }

            String uploadDir = "category-images/" + savedCategory.getId();

            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, imageName, image.getInputStream());

            return ResponseEntity.ok(savedCategory);
        } else {
            if (StringUtils.isEmpty(categoryDTO.getImage())) categoryDTO.setImage(null);

            try {
                return ResponseEntity.ok(categoryService.save(categoryDTO));
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
            }
        }
    }

    private boolean isFileNullAndEmpty(MultipartFile image) {
        if (image == null) {
            return true;
        }
        return image.isEmpty();
    }

    @GetMapping("/{id}/enabled/{status}")
    public ResponseEntity<?> updateCategoryEnabledStatus(@PathVariable("id") Long id,
                                                 @PathVariable("status") boolean status) {
        try {
            categoryService.updateCategoryEnabledStatus(id, status);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCategoryById(@PathVariable("id") Long id) {
        try {
            categoryService.delete(id);
            String dir = "category-images/" + id;
            awsS3Service.removeFolder(dir + "/");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.noContent().build();
    }
}
