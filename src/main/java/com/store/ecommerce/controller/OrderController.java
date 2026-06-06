package com.store.ecommerce.controller;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.request.OrderReturnRequest;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.dto.wrapper.MessageResponseWrapper;
import com.store.ecommerce.dto.wrapper.OrderListWrapper;
import com.store.ecommerce.dto.wrapper.OrderWrapper;
import com.store.ecommerce.dto.wrapper.PagedOrderWrapper;
import com.store.ecommerce.service.OrderService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "APIs for managing orders")
public class OrderController {
    private final OrderService orderService;

    // ================= CUSTOMER =================

    @Operation(
            summary = "Get my orders",
            description = "Retrieve paginated orders for the authenticated customer"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Orders retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedOrderWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiSuccessResponse<PageResponse<OrderDTO>>> getOrderByUser(
            Authentication authentication,
            @RequestParam int pageNum,
            @RequestParam int pageSize,
            @RequestParam String sortField,
            @RequestParam String sortDir) {

        String email = authentication.getName();

        PageResponse<OrderDTO> data;

        if (pageSize < 1) {
            List<OrderDTO> allOrders = orderService.getAllOrdersByCustomerEmail(email, sortField, sortDir);

            data = PageResponse.<OrderDTO>builder()
                    .content(allOrders)
                    .totalItems((long) allOrders.size())
                    .totalPages(1)
                    .build();
        } else {
            Page<OrderDTO> page = orderService.getOrdersByCustomerEmailAndPage(
                    email, pageNum, pageSize, sortField, sortDir);

            data = PageResponse.<OrderDTO>builder()
                    .content(page.getContent())
                    .totalItems(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<OrderDTO>>builder()
                        .success(true)
                        .message("Orders retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Request order return",
            description = "Submit a return request for an order"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Return request submitted",
            content = @Content(schema = @Schema(implementation = OrderWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiSuccessResponse<OrderDTO>> handleOrderReturnRequest(
            Authentication authentication,
            @PathVariable("id") Long orderId,
            @RequestBody OrderReturnRequest returnRequest) {

        OrderDTO order = orderService.setOrderReturnRequested(
                authentication.getName(), orderId, returnRequest);

        return ResponseEntity.ok(
                ApiSuccessResponse.<OrderDTO>builder()
                        .success(true)
                        .message("Return request submitted successfully")
                        .data(order)
                        .build()
        );
    }

    // ================= ADMIN =================

    @Operation(
            summary = "Get orders (Admin)",
            description = "Retrieve orders with pagination, filtering and sorting"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Orders retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedOrderWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageResponse<OrderDTO>>> getOrdersByPage(
            PagingAndSortingHelper helper) {

        PageResponse<OrderDTO> data;

        if (helper.getPageSize() < 1) {
            List<OrderDTO> allOrders = orderService.getAllOrders(
                    helper.getKeyword(),
                    helper.getSortField(),
                    helper.getSortDir()
            );

            data = PageResponse.<OrderDTO>builder()
                    .content(allOrders)
                    .totalItems((long) allOrders.size())
                    .totalPages(1)
                    .build();
        } else {
            Page<OrderDTO> page = orderService.getOrdersByPage(helper);

            data = PageResponse.<OrderDTO>builder()
                    .content(page.getContent())
                    .totalItems(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<OrderDTO>>builder()
                        .success(true)
                        .message("Orders retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get all orders",
            description = "Retrieve all orders without pagination (Admin only)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Orders retrieved successfully",
            content = @Content(schema = @Schema(implementation = OrderListWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<OrderDTO>>> getAllOrders() {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<OrderDTO>>builder()
                        .success(true)
                        .message("Orders retrieved successfully")
                        .data(orderService.getAllOrders())
                        .build()
        );
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieve a specific order (Admin only)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order retrieved successfully",
            content = @Content(schema = @Schema(implementation = OrderWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<OrderDTO>> getOrderById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<OrderDTO>builder()
                        .success(true)
                        .message("Order retrieved successfully")
                        .data(orderService.getOrderById(id))
                        .build()
        );
    }

    @Operation(
            summary = "Create or update order",
            description = "Create or update an order (Admin only)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order saved successfully",
            content = @Content(schema = @Schema(implementation = OrderWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<OrderDTO>> saveOrder(
            @RequestBody OrderDTO orderDTO) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<OrderDTO>builder()
                        .success(true)
                        .message("Order saved successfully")
                        .data(orderService.saveOrder(orderDTO))
                        .build()
        );
    }

    @Operation(
            summary = "Delete order",
            description = "Delete an order by ID (Admin only)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order deleted successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteOrderById(
            @PathVariable("id") Long id) {

        orderService.deleteById(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Order deleted successfully")
                        .data(null)
                        .build()
        );
    }
}
