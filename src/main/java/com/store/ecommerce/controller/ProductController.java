package com.store.ecommerce.controller;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.request.ProductStatusRequest;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.ProductService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.product.ProductCsvExporter;
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
import java.util.Objects;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

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

        ProductDTO productByAlias = productService.getProductByAlias(alias);

        return ResponseEntity.ok(productByAlias);
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getProductByCategoryName(@PathVariable("categoryName") String categoryName,
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

        return ResponseEntity.ok(productService.getProductByID(id));
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
            @RequestBody @Valid ProductStatusRequest request
    ) {

        productService.changeEnabledStatus(id, request.getStatus());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/name-unique")
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
            @RequestPart(name = "extrasImagesFile", required = false) MultipartFile[] extrasImagesFile) throws IOException {

        ProductDTO savedProduct = productService.saveProduct(productDTO, mainImageFile, extrasImagesFile);

        return new ResponseEntity<>(savedProduct, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        String dir = "product-images/" + id;
        awsS3Service.removeFolder(dir + "/");

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportToCsv(HttpServletResponse response) throws IOException {
        List<ProductDTO> listProducts = productService.getAllProducts(0L);
        ProductCsvExporter exporter = new ProductCsvExporter();

        exporter.export(response, listProducts);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
