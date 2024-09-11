package com.store.ecommerce.controller.customer;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.ProductService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("ProductController")
@RequestMapping("/api/customer/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/home")
    public ResponseEntity<List<ProductDTO>> getProductForHomePage() {
        return ResponseEntity.ok(productService.getProductForHomePage());
    }

    @GetMapping("/{alias}")
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
}
