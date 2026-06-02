package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.OrderDetailDTO;
import com.store.ecommerce.dto.OrderTrackDTO;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = Mappers.getMapper(OrderMapper.class);
    }

    @Test
    void testToOrderDTO_AllFieldsMapped() {
        // Given
        User user = new User();
        user.setId(1L);
        // (Giả sử user đã có firstName/lastName để getFullName() hoạt động tốt)

        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setFirstName("John");
        order.setLastName("Doe");
        order.setPhoneNumber("+1234567890");
        order.setAddressLine1("123 Main St");
        order.setAddressLine2("Apt 4B");
        order.setCity("New York");
        order.setState("NY");
        order.setCountry("USA");
        order.setPostalCode("10001");
        order.setShippingCost(10.0f);
        order.setProductCost(100.0f);
        order.setSubtotal(110.0f);
        order.setTax(5.5f);
        order.setTotal(115.5f);
        order.setOrderTime(new Date());
        order.setDeliverDays(5);
        order.setDeliverDate(new Date());

        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PROCESSING);

        List<OrderTrack> orderTracks = new ArrayList<>();
        OrderTrack track = new OrderTrack();
        track.setId(1L);
        track.setStatus(OrderStatus.PAID);
        track.setUpdatedTime(new Date());
        track.setNotes("Order placed successfully");
        orderTracks.add(track);
        order.setOrderTrack(orderTracks);

        Set<OrderDetail> orderDetails = new HashSet<>();
        OrderDetail detail = new OrderDetail();
        detail.setId(1L);
        detail.setQuantity(2);
        detail.setProductCost(50.0f);
        detail.setUnitPrice(50.0f);
        detail.setShippingCost(5.0f);
        detail.setSubtotal(105.0f);

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setMainImage("product.jpg");
        product.setCost(30.0f);
        detail.setProduct(product);

        orderDetails.add(detail);
        order.setOrderDetails(orderDetails);

        // When
        OrderDTO orderDTO = orderMapper.toOrderDTO(order);

        // Then
        assertNotNull(orderDTO);
        assertEquals(1L, orderDTO.getId());
        assertEquals(1L, orderDTO.getUserId());
        assertEquals("John", orderDTO.getFirstName());
        assertEquals("Doe", orderDTO.getLastName());
        assertEquals("+1234567890", orderDTO.getPhoneNumber());
        assertEquals("123 Main St", orderDTO.getAddressLine1());
        assertEquals("Apt 4B", orderDTO.getAddressLine2());
        assertEquals("New York", orderDTO.getCity());
        assertEquals("NY", orderDTO.getState());
        assertEquals("USA", orderDTO.getCountry());
        assertEquals("10001", orderDTO.getPostalCode());
        assertEquals(10.0f, orderDTO.getShippingCost());
        assertEquals(100.0f, orderDTO.getProductCost());
        assertEquals(110.0f, orderDTO.getSubtotal());
        assertEquals(5.5f, orderDTO.getTax());
        assertEquals(115.5f, orderDTO.getTotal());
        assertEquals(5, orderDTO.getDeliverDays());
        assertEquals(PaymentMethod.CREDIT_CARD, orderDTO.getPaymentMethod());
        assertEquals(OrderStatus.PROCESSING, orderDTO.getStatus());

        assertNotNull(orderDTO.getOrderDetails());
        assertEquals(1, orderDTO.getOrderDetails().size());

        OrderDetailDTO detailDTO = orderDTO.getOrderDetails().iterator().next();
        assertEquals(1L, detailDTO.getId());
        assertEquals(2, detailDTO.getQuantity());

        assertEquals(50.0f, detailDTO.getProductCostTotal());

        assertEquals(50.0f, detailDTO.getUnitPrice());
        assertEquals(5.0f, detailDTO.getShippingCost());
        assertEquals(105.0f, detailDTO.getSubtotal());
        assertEquals(1L, detailDTO.getProductId());
        assertEquals("Test Product", detailDTO.getProductName());
        assertEquals("product.jpg", detailDTO.getProductImageName());
        assertEquals(30.0f, detailDTO.getProductCost());
    }

    @Test
    void testToOrderDTO_NullInput() {
        OrderDTO orderDTO = orderMapper.toOrderDTO(null);
        assertNull(orderDTO);
    }

    @Test
    void testToOrderDTO_NullNestedObjects() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(null);
        order.setOrderDetails(null);
        order.setOrderTrack(null);

        OrderDTO orderDTO = orderMapper.toOrderDTO(order);

        assertNotNull(orderDTO);
        assertEquals(1L, orderDTO.getId());
        assertNull(orderDTO.getUserId());
        assertNull(orderDTO.getOrderDetails());
    }

    @Test
    void testToOrderDetailDTO() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Widget");
        product.setMainImage("widget.png");
        product.setCost(25.0f);

        OrderDetail detail = new OrderDetail();
        detail.setId(1L);
        detail.setQuantity(3);
        detail.setProductCost(75.0f);
        detail.setUnitPrice(25.0f);
        detail.setShippingCost(3.0f);
        detail.setSubtotal(78.0f);
        detail.setProduct(product);

        OrderDetailDTO detailDTO = orderMapper.toOrderDetailDTO(detail);

        assertNotNull(detailDTO);
        assertEquals(1L, detailDTO.getId());
        assertEquals(3, detailDTO.getQuantity());
        assertEquals(75.0f, detailDTO.getProductCostTotal());
        assertEquals(25.0f, detailDTO.getUnitPrice());
        assertEquals(3.0f, detailDTO.getShippingCost());
        assertEquals(78.0f, detailDTO.getSubtotal());
        assertEquals(1L, detailDTO.getProductId());
        assertEquals("Widget", detailDTO.getProductName());
        assertEquals("widget.png", detailDTO.getProductImageName());
        assertEquals(25.0f, detailDTO.getProductCost());
    }

    @Test
    void testToOrderTrackDTO() {
        Order order = new Order();
        order.setId(1L);

        OrderTrack track = new OrderTrack();
        track.setId(1L);
        track.setOrder(order);
        track.setStatus(OrderStatus.SHIPPING);
        track.setUpdatedTime(new Date());
        track.setNotes("Package shipped via FedEx");

        OrderTrackDTO trackDTO = orderMapper.toOrderTrackDTO(track);

        assertNotNull(trackDTO);
        assertEquals(1L, trackDTO.getId());
        assertEquals(1L, trackDTO.getOrderId());
        assertEquals(OrderStatus.SHIPPING, trackDTO.getStatus());
        assertEquals("Package shipped via FedEx", trackDTO.getNotes());
        assertNotNull(trackDTO.getUpdatedTime());
    }

    @Test
    void testToOrderDTO_MultipleOrderDetails() {
        Order order = new Order();
        order.setId(1L);

        Set<OrderDetail> orderDetails = new HashSet<>();

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setMainImage("p1.jpg");
        product1.setCost(10.0f);

        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1L);
        detail1.setQuantity(1);
        detail1.setProductCost(10.0f);
        detail1.setUnitPrice(10.0f);
        detail1.setShippingCost(2.0f);
        detail1.setSubtotal(12.0f);
        detail1.setProduct(product1);
        orderDetails.add(detail1);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setMainImage("p2.jpg");
        product2.setCost(20.0f);

        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2L);
        detail2.setQuantity(2);
        detail2.setProductCost(40.0f);
        detail2.setUnitPrice(20.0f);
        detail2.setShippingCost(3.0f);
        detail2.setSubtotal(43.0f);
        detail2.setProduct(product2);
        orderDetails.add(detail2);

        order.setOrderDetails(orderDetails);

        OrderDTO orderDTO = orderMapper.toOrderDTO(order);

        assertNotNull(orderDTO);
        assertNotNull(orderDTO.getOrderDetails());
        assertEquals(2, orderDTO.getOrderDetails().size());
    }

    @Test
    void testToOrderDTO_WithUserNull() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(null);

        OrderDTO orderDTO = orderMapper.toOrderDTO(order);

        assertNotNull(orderDTO);
        assertEquals(1L, orderDTO.getId());
        assertNull(orderDTO.getUserId());
        assertNull(orderDTO.getUserFullName());
    }
}