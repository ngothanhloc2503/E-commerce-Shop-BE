package com.store.ecommerce.controller;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.request.OrderReturnRequest;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.OrderService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getOrderByUser(Authentication authentication,
                                            @RequestParam(name = "pageNum") int pageNum,
                                            @RequestParam(name = "pageSize") int pageSize,
                                            @RequestParam(name = "sortField") String sortField,
                                            @RequestParam(name = "sortDir") String sortDir) {
        String email = authentication.getName();
        if (pageSize < 1) {
            try {
                List<OrderDTO> allOrders = orderService.getAllOrdersByCustomerEmail(email, sortField, sortDir);
                return ResponseEntity.ok(PagedResponseDTO.builder()
                        .content(allOrders)
                        .totalItems((long) allOrders.size())
                        .totalPages(1).build());
            } catch (NotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            }
        }

        try {
            Page<OrderDTO> ordersByPage = orderService.getOrdersByCustomerEmailAndPage(email, pageNum, pageSize, sortField, sortDir);
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(ordersByPage.getContent())
                    .totalItems(ordersByPage.getTotalElements())
                    .totalPages(ordersByPage.getTotalPages()).build());
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> handleOrderReturnRequest(Authentication authentication,
                                                      @PathVariable(name = "id") Long orderId,
                                                      @RequestBody OrderReturnRequest returnRequest) {
        try {
            return ResponseEntity.ok(orderService.setOrderReturnRequested(authentication.getName(), orderId, returnRequest));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrdersByPage(PagingAndSortingHelper helper) {
        if (helper.getPageSize() < 1) {
            List<OrderDTO> allOrders = orderService.getAllOrders(helper.getKeyword(), helper.getSortField(), helper.getSortDir());
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(allOrders)
                    .totalItems((long) allOrders.size())
                    .totalPages(1).build());
        }

        Page<OrderDTO> ordersByPage = orderService.getOrdersByPage(helper);
        return ResponseEntity.ok(PagedResponseDTO.builder()
                .content(ordersByPage.getContent())
                .totalItems(ordersByPage.getTotalElements())
                .totalPages(ordersByPage.getTotalPages()).build());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrderById(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveOrder(@RequestBody OrderDTO orderDTO) {
        try {
            return ResponseEntity.ok(orderService.saveOrder(orderDTO));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteOrderById(@PathVariable("id") Long id) {
        try {
            orderService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
