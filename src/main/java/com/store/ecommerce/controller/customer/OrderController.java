package com.store.ecommerce.controller.customer;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.request.OrderReturnRequest;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.security.jwt.JwtUtil;
import com.store.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("OrderController")
@RequestMapping("/api/customer/orders")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    private final JwtUtil jwtUtil;

    @GetMapping("")
    public ResponseEntity<?> getOrderByUser(@RequestHeader("Authorization") String jwt,
                                            @RequestParam(name = "pageNum") int pageNum,
                                            @RequestParam(name = "pageSize") int pageSize,
                                            @RequestParam(name = "sortField") String sortField,
                                            @RequestParam(name = "sortDir") String sortDir) {
        String email = jwtUtil.extractSubject(jwt);
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

    @PostMapping("/return")
    public ResponseEntity<?> handleOrderReturnRequest(@RequestHeader("Authorization") String jwt,
                                                      OrderReturnRequest returnRequest) {
        try {
            return ResponseEntity.ok(orderService.setOrderReturnRequested(jwtUtil.extractSubject(jwt), returnRequest));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
