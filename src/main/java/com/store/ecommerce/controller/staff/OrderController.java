package com.store.ecommerce.controller.staff;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.OrderService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("ManageOrderController")
@RequestMapping("/api/staff/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("")
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveOrder(@RequestBody OrderDTO orderDTO) {
        try {
            return ResponseEntity.ok(orderService.saveOrder(orderDTO));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteOrderById(@PathVariable("id") Long id) {
        try {
            orderService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
