package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.OrderDetailDTO;
import com.store.ecommerce.dto.OrderTrackDTO;
import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.OrderDetail;
import com.store.ecommerce.entity.OrderTrack;
import com.store.ecommerce.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper
public interface OrderMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "addressLine1", target = "addressLine1")
    @Mapping(source = "addressLine2", target = "addressLine2")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "state", target = "state")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "postalCode", target = "postalCode")
    @Mapping(source = "shippingCost", target = "shippingCost")
    @Mapping(source = "productCost", target = "productCost")
    @Mapping(source = "subtotal", target = "subtotal")
    @Mapping(source = "tax", target = "tax")
    @Mapping(source = "total", target = "total")
    @Mapping(source = "orderTime", target = "orderTime", dateFormat = "yyyy-MM-ddThh:mm:ss")
    @Mapping(source = "deliverDays", target = "deliverDays")
    @Mapping(source = "deliverDate", target = "deliverDate", dateFormat = "yyyy-MM-ddThh:mm:ss")
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "userFullName", target = "userFullName")
    @Mapping(source = "orderTrack", target = "orderTrack")
    @Mapping(source = "orderDetails", target = "orderDetails", qualifiedByName = "orderDetailsToOrderDetailDTOs")
    public OrderDTO toOrderDTO(Order order);

    @Named("orderDetailsToOrderDetailDTOs")
    public static Set<OrderDetailDTO> orderDetailsToOrderDetailDTOs(Set<OrderDetail> orderDetails) {
        if ( orderDetails == null ) {
            return null;
        }

        Set<OrderDetailDTO> orderDetailDTOs = new HashSet<>();
        for ( OrderDetail orderDetail : orderDetails ) {
            OrderDetailDTO orderDetailDTO = new OrderDetailDTO();
            orderDetailDTO.setId(orderDetail.getId());
            orderDetailDTO.setQuantity(orderDetail.getQuantity());
            orderDetailDTO.setProductCostTotal(orderDetail.getProductCost());
            orderDetailDTO.setUnitPrice(orderDetail.getUnitPrice());
            orderDetailDTO.setShippingCost(orderDetail.getShippingCost());
            orderDetailDTO.setSubtotal(orderDetail.getSubtotal());

            Product product = orderDetail.getProduct();
            orderDetailDTO.setProductId(product.getId());
            orderDetailDTO.setProductName(product.getName());
            orderDetailDTO.setProductImageName(product.getMainImage());
            orderDetailDTO.setProductCost(product.getCost());

            orderDetailDTOs.add(orderDetailDTO);
        }

        return orderDetailDTOs;
    }
}
