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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "orderTime", target = "orderTime", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(source = "deliverDate", target = "deliverDate", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    OrderDTO toOrderDTO(Order order);

    @Mapping(source = "productCost", target = "productCostTotal")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.mainImage", target = "productImageName")
    @Mapping(source = "product.cost", target = "productCost")
    OrderDetailDTO toOrderDetailDTO(OrderDetail orderDetail);

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "updatedTime", target = "updatedTime", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    OrderTrackDTO toOrderTrackDTO(OrderTrack orderTrack);
}
