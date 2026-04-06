package com.store.ecommerce.controller;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.BrandService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.brand.BrandCsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/brands")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    private final AWSS3Service awsS3Service;

    @GetMapping("")
    public ResponseEntity<PagedResponseDTO> getBrandByPage(PagingAndSortingHelper helper) {
        if (helper.getPageSize() < 1) {
            List<BrandDTO> allBrands = brandService.getAllBrands(helper.getKeyword(), helper.getSortField(), helper.getSortDir());
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(allBrands)
                    .totalPages(1)
                    .totalItems((long) allBrands.size()).build());
        }

        Page<BrandDTO> page = brandService.getBrandByPage(helper);
        return ResponseEntity.ok(PagedResponseDTO.builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements()).build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<BrandDTO>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBrandById(@PathVariable("id") Long id) {

        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @GetMapping("/category/{categoryID}")
    public ResponseEntity<List<BrandDTO>> getBrandByCategory(@PathVariable("categoryID") Long categoryID) {
        return ResponseEntity.ok(brandService.getBrandByCategory(categoryID));
    }

    @PostMapping("")
    public ResponseEntity<?> saveBrand(
            @RequestPart("brand") BrandDTO brandDTO,
            @RequestPart(name = "logo", required = false) MultipartFile logo) throws IOException {

        // Update brand
        BrandDTO savedBrand = brandService.saveBrand(brandDTO, logo);

        return ResponseEntity.ok(savedBrand);
    }

    @GetMapping("/name-unique")
    public boolean checkNameUnique(@RequestParam("id") Long id,
                                   @RequestParam("name") String name) {
        return brandService.isNameUnique(id, name);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable("id") Long id) {

        brandService.deleteBrand(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<?> exportToCsv(HttpServletResponse response) throws IOException {
        List<BrandDTO> listBrands = brandService.getAllBrands();
        BrandCsvExporter exporter = new BrandCsvExporter();

        exporter.export(response, listBrands);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
