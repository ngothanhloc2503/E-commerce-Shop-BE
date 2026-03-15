package com.store.ecommerce.controller;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.ProductService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.product.ProductCsvExporter;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.Objects;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final AWSS3Service awsS3Service;

    @GetMapping("/home")
    public ResponseEntity<List<ProductDTO>> getProductForHomePage() {
        return ResponseEntity.ok(productService.getProductForHomePage());
    }

    @GetMapping("/alias/{alias}")
    public ResponseEntity<?> getProductByAlias(@PathVariable("alias") String alias) {
        try {
            ProductDTO productByAlias = productService.getProductByAlias(alias);
            return ResponseEntity.ok(productByAlias);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/category")
    public ResponseEntity<?> getProductByCategoryName(@RequestParam("categoryName") String categoryName,
                                                  @RequestParam("pageNum") int pageNum) {
        Page<ProductDTO> page = productService.getProductByCategoryName(categoryName, pageNum);
        return ResponseEntity.ok(PagedResponseDTO.builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements()).build());
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponseDTO> getProductByPage(PagingAndSortingHelper helper,
                                                             @RequestParam("categoryID") Long categoryID) {
        if (helper.getPageSize() < 1) {
            List<ProductDTO> allProducts = productService.getAllProducts(categoryID,
                    helper.getKeyword(), helper.getSortField(), helper.getSortDir());
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(allProducts)
                    .totalPages(1)
                    .totalItems((long) allProducts.size()).build());
        } else {
            Page<ProductDTO> productByPage = productService.getProductByPage(helper, categoryID);
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(productByPage.getContent())
                    .totalPages(productByPage.getTotalPages())
                    .totalItems(productByPage.getTotalElements()).build());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProductByID(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(productService.getProductByID(id));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts(0L));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeEnabledStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") boolean status
    ) {
        try {
            productService.changeEnabledStatus(id, status);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-name-unique")
    @PreAuthorize("hasRole('ADMIN')")
    public boolean checkNameUnique(@RequestParam("id") Long id,
                                   @RequestParam("name") String name) {
        return productService.isNameUnique(id, name);
    }

    @PostMapping(path = "", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveProduct(
            @RequestPart("product") ProductDTO productDTO,
            @RequestPart(name = "mainImageFile", required = false) MultipartFile mainImageFile,
            @RequestPart(name = "listExtrasImageFile", required = false) MultipartFile[] listExtrasImageFile) {

        try {
            setMainImageName(productDTO, mainImageFile);

            ProductDTO savedProduct = productService.saveProduct(productDTO, mainImageFile);

            saveUploadImages(mainImageFile, listExtrasImageFile, savedProduct);
            deleteExtraImagesWereRemovedOnForm(savedProduct);

            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable("id") Long id) {
        try {
            productService.deleteProduct(id);
            String dir = "product-images/" + id;
            awsS3Service.removeFolder(dir + "/");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportToCsv(HttpServletResponse response) {
        List<ProductDTO> listProducts = productService.getAllProducts(0L);
        ProductCsvExporter exporter = new ProductCsvExporter();

        try {
            exporter.export(response, listProducts);
        } catch (IOException e) {
            return new ResponseEntity<>("Error while writing CSV file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void deleteExtraImagesWereRemovedOnForm(ProductDTO productDTO) {
        String extraImageDir = "product-images/" + productDTO.getId() + "/extras/";
        List<String> listObjectKeys = awsS3Service.listFolder(extraImageDir);

        for (String objectKey : listObjectKeys) {
            int lastIndexOfSlash = objectKey.lastIndexOf("/");
            String fileName = objectKey.substring(lastIndexOfSlash + 1, objectKey.length());

            if (!productDTO.containsImageName(fileName)) {
                awsS3Service.deleteFile(objectKey);
            }
        }
    }

    private void saveUploadImages(MultipartFile mainImageMultipart, MultipartFile[] listExtrasImageFile,
                                  ProductDTO savedProduct) throws IOException {
        if (!isImageNullOrEmpty(mainImageMultipart)) {
            String fileName = StringUtils.cleanPath(mainImageMultipart.getOriginalFilename());
            String uploadDir = "product-images/" + savedProduct.getId();

            List<String> listObjectKeys = awsS3Service.listFolder(uploadDir + "/");
            for (String objectKey : listObjectKeys) {
                if (!objectKey.contains("/extras")) {
                    awsS3Service.deleteFile(objectKey);
                }
            }

            awsS3Service.uploadFile(uploadDir, fileName, mainImageMultipart.getInputStream());
        }

        if (listExtrasImageFile == null) return;

        if (listExtrasImageFile.length > 0) {
            String uploadDir = "product-images/" + savedProduct.getId() + "/extras";

            for (MultipartFile extrasImageFile: listExtrasImageFile) {
                if (!isImageNullOrEmpty(extrasImageFile)) {
                    String fileName = StringUtils.cleanPath(extrasImageFile.getOriginalFilename());
                    awsS3Service.uploadFile(uploadDir, fileName, extrasImageFile.getInputStream());
                } else {
                    continue;
                }
            }
        }
    }

    private void setMainImageName(ProductDTO productDTO, MultipartFile mainImageMultipart) {
        if (!isImageNullOrEmpty(mainImageMultipart)) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(mainImageMultipart.getOriginalFilename()));
            productDTO.setMainImage(fileName);
        }
        else {
            if (StringUtils.isEmpty(productDTO.getMainImage())) productDTO.setMainImage(null);
        }
    }

    private boolean isImageNullOrEmpty(MultipartFile image) {
        if (image == null) {
            return true;
        }
        return image.isEmpty();
    }
}
