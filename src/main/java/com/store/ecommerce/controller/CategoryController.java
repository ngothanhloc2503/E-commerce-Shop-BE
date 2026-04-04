package com.store.ecommerce.controller;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.dto.request.CategoryStatusRequest;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.CategoryService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.category.CategoryCsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    private final AWSS3Service awsS3Service;

    @GetMapping("/all")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

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

    @GetMapping("/by-name/{name}")
    public ResponseEntity<?> getCategoryByName(@PathVariable("name") String name) {

        return ResponseEntity.ok(categoryService.getCategoryByName(name));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCategoryById(@PathVariable("id") Long id) {

        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/name-unique")
    @PreAuthorize("hasRole('ADMIN')")
    public boolean checkUniqueName(@RequestParam("id") Long id,
                                   @RequestParam("name") String name) {
        return categoryService.isNameUnique(id, name);
    }

    @PostMapping(path = "", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveCategory(@RequestPart("category") CategoryDTO categoryDTO,
                                          @RequestPart(name = "image", required = false) MultipartFile image)
            throws IOException {


        // Handle image
        if (!isFileNullOrEmpty(image)) {
            String originalName = image.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;
            categoryDTO.setImage(fileName);
        } else if (StringUtils.isEmpty(categoryDTO.getImage())) {
            categoryDTO.setImage(null);
        }

        // Update category
        CategoryDTO savedCategory = categoryService.save(categoryDTO);

        // Upload image if exists
        if (!isFileNullOrEmpty(image)) {
            String uploadDir = "category-images/" + savedCategory.getId();

            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, categoryDTO.getImage(), image.getInputStream());
        }

        return ResponseEntity.ok(savedCategory);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategoryEnabledStatus(@PathVariable("id") Long id,
                                                         @RequestBody @Valid CategoryStatusRequest request) {

        categoryService.updateCategoryEnabledStatus(id, request.getStatus());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategoryById(@PathVariable("id") Long id) {

        categoryService.delete(id);
        String dir = "category-images/" + id;
        awsS3Service.removeFolder(dir + "/");

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportToCsv(HttpServletResponse response) throws IOException {
        List<CategoryDTO> listCategories = categoryService.getAllCategories();
        CategoryCsvExporter exporter = new CategoryCsvExporter();

        exporter.export(response, listCategories);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
