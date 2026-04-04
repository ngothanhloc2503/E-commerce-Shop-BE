package com.store.ecommerce.controller;

import com.store.ecommerce.dto.request.CodSupportRequest;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.entity.ShippingRate;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.ShippingRateService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-rates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ShippingRateController {
    private final ShippingRateService shippingRateService;

    @GetMapping(path = "")
    public ResponseEntity<PagedResponseDTO> getShippingRatesByPage(PagingAndSortingHelper helper) {
        if (helper.getPageSize() < 1) {
            List<ShippingRate> shippingRates = shippingRateService.getAllShippingRates(helper.getKeyword(), helper.getSortField(), helper.getSortDir());
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(shippingRates)
                    .totalPages(1)
                    .totalItems((long) shippingRates.size()).build());
        }
        Page<ShippingRate> page = shippingRateService.getShippingRatesByPage(helper);

        return ResponseEntity.ok(PagedResponseDTO.builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements()).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getShippingRateById(@PathVariable("id") Long id) {

        return ResponseEntity.ok(shippingRateService.getShippingRateById(id));
    }

    @PostMapping("")
    public ResponseEntity<?> saveShippingRate(ShippingRate shippingRate) {
        try {
            return ResponseEntity.ok(shippingRateService.saveShippingRate(shippingRate));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/cod")
    public ResponseEntity<?> updateCodSupport(@PathVariable("id") Long id,
                                              @RequestBody @Valid CodSupportRequest request) {

        return ResponseEntity.ok(shippingRateService.updateCodSupported(id, request.isSupported()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShippingRate(@PathVariable(name = "id") Long id) {

        shippingRateService.deleteShippingRate(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
