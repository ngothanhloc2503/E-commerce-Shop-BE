package com.store.ecommerce.service;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.request.OrderReturnRequest;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {
    List<OrderDTO> getAllOrders();

    List<OrderDTO> getAllOrders(String keyword, String sortField, String sortDir);

    Page<OrderDTO> getOrdersByPage(PagingAndSortingHelper helper);

    OrderDTO getOrderById(Long orderId) throws NotFoundException;

    OrderDTO saveOrder(OrderDTO orderDTO) throws ConflictException, NotFoundException;

    void deleteById(Long orderId) throws NotFoundException;

    // For Customer
    OrderDTO createOrder(String email, PaymentMethod paymentMethod) throws NotFoundException;

    List<OrderDTO> getAllOrdersByCustomerEmail(String email, String sortField, String sortDir) throws NotFoundException;

    Page<OrderDTO> getOrdersByCustomerEmailAndPage(String email, int pageNum, int pageSize, String sortField, String sortDir) throws NotFoundException;

    OrderDTO setOrderReturnRequested(String email, Long id, OrderReturnRequest request) throws ConflictException, NotFoundException;
}
