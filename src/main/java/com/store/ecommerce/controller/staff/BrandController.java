package com.store.ecommerce.controller.staff;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.BrandService;
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

@RestController
@RequestMapping("/api/staff/brands")
@PreAuthorize("hasRole('ADMIN')")
public class BrandController {
    @Autowired
    private BrandService brandService;

    @Autowired
    private AWSS3Service awsS3Service;

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
        try {
            return ResponseEntity.ok(brandService.getBrandById(id));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/category/{categoryID}")
    public ResponseEntity<List<BrandDTO>> getBrandByCategory(@PathVariable("categoryID") Long categoryID) {
        return ResponseEntity.ok(brandService.getBrandByCategory(categoryID));
    }

    @PostMapping(path = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveBrand(
            @RequestPart("brand") BrandDTO brandDTO,
            @RequestPart(name = "logo", required = false) MultipartFile logo) throws IOException {
        if (!isLogoNullOrEmpty(logo)) {
            String logoName = StringUtils.cleanPath(logo.getOriginalFilename());
            brandDTO.setLogo(logoName);

            BrandDTO savedBrand = null;
            try {
                savedBrand = brandService.saveBrand(brandDTO);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
            }

            String uploadDir = "brand-logos/" + savedBrand.getId();

            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, logoName, logo.getInputStream());

            return ResponseEntity.ok(savedBrand);
        } else {
            if (StringUtils.isEmpty(brandDTO.getLogo())) brandDTO.setLogo(null);

            try {
                return ResponseEntity.ok(brandService.saveBrand(brandDTO));
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
            }
        }
    }

    @GetMapping("/check-name-unique")
    public boolean checkNameUnique(@RequestParam("id") Long id,
                                   @RequestParam("name") String name) {
        return brandService.isNameUnique(id, name);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable("id") Long id) {
        try {
            brandService.deleteBrand(id);
            String dir = "brand-logos/" + id;
            awsS3Service.removeFolder(dir + "/");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.noContent().build();
    }

    private boolean isLogoNullOrEmpty(MultipartFile logo) {
        if (logo == null) {
            return true;
        }
        return logo.isEmpty();
    }
}
