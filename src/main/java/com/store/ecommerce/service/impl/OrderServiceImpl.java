package com.store.ecommerce.service.impl;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.OrderDetailDTO;
import com.store.ecommerce.dto.OrderTrackDTO;
import com.store.ecommerce.dto.request.OrderReturnRequest;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.OrderMapper;
import com.store.ecommerce.repository.*;
import com.store.ecommerce.service.*;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final ProductRepository productRepository;
    private final AddressService addressService;
    private final ShippingRateService shippingRateService;
    private final CartService cartService;
    private final AWSS3Service awsS3Service;

    @Override
    public List<OrderDTO> getAllOrders() {
        List<Order> listOrders = orderRepository.findAll();
        return listOrders.stream().map(orderMapper::toOrderDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getAllOrders(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<Order> listOrders = orderRepository.findAll(keyword, sort);
        return listOrders.stream().map(orderMapper::toOrderDTO).collect(Collectors.toList());
    }

    @Override
    public Page<OrderDTO> getOrdersByPage(PagingAndSortingHelper helper) {
        Page<Order> pageOrders = (Page<Order>) helper.getPageEntities(orderRepository);
        return pageOrders.map(orderMapper::toOrderDTO);
    }

    @Override
    public OrderDTO getOrderById(Long orderId) throws NotFoundException {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new NotFoundException("Could not find any order with ID: " + orderId));

        OrderDTO orderDTO = orderMapper.toOrderDTO(order);
        setImagePathForOrderDetails(orderDTO);
        return orderDTO;
    }

    @Override
    public OrderDTO saveOrder(OrderDTO orderDTO) throws ConflictException, NotFoundException {
        Order orderInDB = orderRepository.findById(orderDTO.getId()).orElseThrow(
                () -> new NotFoundException("Could not find any order with id: " + orderDTO.getId()));

        countryRepository.findByNameIgnoreCase(orderDTO.getCountry()).orElseThrow(
                () -> new NotFoundException("Could not find any country with name: " + orderDTO.getCountry()));

        stateRepository.findByCountryAndNameIgnoreCase(orderDTO.getCountry(), orderDTO.getState()).orElseThrow(
                () -> new NotFoundException("Could not find any state " + orderDTO.getState()  + " in country " + orderDTO.getCountry()));

        if (!Objects.equals(orderInDB.getUser().getId(), orderDTO.getUserId())) {
            throw new ConflictException("Could not change user of order");
        }

        // Update Address
        setAddressFromOrderDTO(orderInDB, orderDTO);

        // Update Info
        orderInDB.setShippingCost(orderDTO.getShippingCost());
        orderInDB.setSubtotal(orderDTO.getSubtotal());
        orderInDB.setTax(orderDTO.getTax());
        orderInDB.setTotal(orderDTO.getShippingCost() + orderDTO.getSubtotal() + orderDTO.getTax());
        orderInDB.setProductCost(orderDTO.getProductCost());
        orderInDB.setStatus(orderDTO.getStatus());
        orderInDB.setPaymentMethod(orderDTO.getPaymentMethod());
        orderInDB.setOrderTime(orderDTO.getOrderTime());
        orderInDB.setDeliverDays(orderDTO.getDeliverDays());
        orderInDB.setDeliverDate(getDeliverDate(orderDTO.getOrderTime(), orderDTO.getDeliverDays()));

        setOrderDetailsForOrder(orderInDB, orderDTO);
        setOrderTrackForOrder(orderInDB, orderDTO);

        Order savedOrder = orderRepository.save(orderInDB);
        return orderMapper.toOrderDTO(savedOrder);
    }

    private void setAddressFromOrderDTO(Order orderInDB, OrderDTO orderDTO) {
        orderInDB.setFirstName(orderDTO.getFirstName());
        orderInDB.setLastName(orderDTO.getLastName());
        orderInDB.setPhoneNumber(orderDTO.getPhoneNumber());
        orderInDB.setAddressLine1(orderDTO.getAddressLine1());
        orderInDB.setAddressLine2(orderDTO.getAddressLine2());
        orderInDB.setCity(orderDTO.getCity());
        orderInDB.setCountry(orderDTO.getCountry());
        orderInDB.setState(orderDTO.getState());
        orderInDB.setPostalCode(orderDTO.getPostalCode());
    }

    private void setOrderDetailsForOrder(Order orderInDB, OrderDTO orderDTO) throws NotFoundException {
        Long maxId = 0L;
        for (OrderDetail orderDetail : orderInDB.getOrderDetails()) {
            if (orderDetail.getId() > maxId) {
                maxId = orderDetail.getId();
            }
        }

        orderInDB.getOrderDetails().clear();
        for (OrderDetailDTO orderDetailDTO : orderDTO.getOrderDetails()) {
            Product product = productRepository.findById(orderDetailDTO.getProductId()).orElseThrow(
                    () -> new NotFoundException("Could not find any product with ID: " + orderDetailDTO.getProductId()));

            // Create order detail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(orderInDB);
            orderDetail.setProduct(product);
            orderDetail.setQuantity(orderDetailDTO.getQuantity());
            orderDetail.setUnitPrice(orderDetailDTO.getUnitPrice());
            orderDetail.setProductCost(orderDetailDTO.getProductCost() * orderDetailDTO.getQuantity());
            orderDetail.setSubtotal(orderDetailDTO.getSubtotal());
            orderDetail.setShippingCost(orderDetailDTO.getShippingCost());

            orderInDB.getOrderDetails().add(orderDetail);
        }
    }

    private void setOrderTrackForOrder(Order orderInDB, OrderDTO orderDTO) {
        Long maxId = 0L;
        for (OrderTrack track : orderInDB.getOrderTrack()) {
            if (track.getId() > maxId) {
                maxId = track.getId();
            }
        }

        orderInDB.getOrderTrack().clear();
        for (OrderTrackDTO orderTrackDTO : orderDTO.getOrderTrack()) {
            OrderTrack track = new OrderTrack();
            track.setOrder(orderInDB);
            track.setId(orderTrackDTO.getId() > maxId ? 0 : orderTrackDTO.getId());
            track.setStatus(orderTrackDTO.getStatus());
            track.setNotes(orderTrackDTO.getNotes());
            track.setUpdatedTime(orderTrackDTO.getUpdatedTime());
            orderInDB.getOrderTrack().add(track);
        }
    }

    @Override
    public void deleteById(Long orderId) throws NotFoundException {
        orderRepository.findById(orderId).orElseThrow(
                () -> new NotFoundException("Could not find any order with ID:" + orderId));
        orderRepository.deleteById(orderId);
    }

    private OrderDTO setImagePathForOrderDetails(OrderDTO orderDTO) {
        Set<OrderDetailDTO> orderDetails = orderDTO.getOrderDetails();
        for (OrderDetailDTO orderDetailDTO : orderDetails) {
            String dir = "product-images/" + orderDetailDTO.getProductId();
            orderDetailDTO.setProductImagePath(awsS3Service.getImagePath(dir, orderDetailDTO.getProductImageName()));
        }

        return orderDTO;
    }

    // For Customer
    @Override
    public OrderDTO createOrder(String email, PaymentMethod paymentMethod) throws NotFoundException {
        User userByEmail = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        // Create new order
        Order order = new Order();

        // Set customer for order
        order.setUser(userByEmail);

        // Get cart by user email
        Cart cart = cartRepository.findByUserEmail(email);
        if (cart == null) {
            throw new NotFoundException("No item found!");
        }

        // Get default address
        Address defaultAddress = addressService.getDefaultAddress(email);
        order.setAddress(defaultAddress);

        // Get shipping rate from default address
        ShippingRate shippingRate = shippingRateService.getShippingRateByCountryAndState(
                defaultAddress.getCountry(), defaultAddress.getState());
        if (shippingRate == null) {
            throw new NotFoundException("Default address does not supported for shipping!");
        }

        // Set info
        float shippingCostTotal = calculateShippingCost(cart.getItems(), shippingRate);
        order.setShippingCost(shippingCostTotal);
        order.setSubtotal(cart.getTotal());
        order.setTax(0.0f);
        order.setTotal(cart.getTotal() + shippingCostTotal);
        order.setProductCost(calculateProductCost(cart.getItems(), shippingRate));
        order.setPaymentMethod(paymentMethod);
        order.setOrderTime(new Date());
        order.setDeliverDays(shippingRate.getDays());
        order.setDeliverDate(getDeliverDate(new Date(), shippingRate.getDays()));

        // Create order track
        OrderTrack newTrack = new OrderTrack();
        newTrack.setOrder(order);
        newTrack.setStatus(OrderStatus.NEW);
        newTrack.setNotes(OrderStatus.NEW.defaultDescription());
        newTrack.setUpdatedTime(new Date());
        order.getOrderTrack().add(newTrack);

        // Set order status
        if (paymentMethod.equals(PaymentMethod.PAYPAL)) {
            order.setStatus(OrderStatus.PAID);

            // Add paid track for order paid by PayPal
            OrderTrack paidTrack = new OrderTrack();
            paidTrack.setOrder(order);
            paidTrack.setStatus(OrderStatus.PAID);
            paidTrack.setNotes(OrderStatus.PAID.defaultDescription());
            paidTrack.setUpdatedTime(new Date());
            order.getOrderTrack().add(paidTrack);
        } else {
            order.setStatus(OrderStatus.NEW);
        }

        // Set orderDetails from cartItems
        Set<OrderDetail> orderDetails = order.getOrderDetails();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            // Create order detail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(product);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setUnitPrice(product.getDiscountPrice());
            orderDetail.setProductCost(product.getCost() * item.getQuantity());
            orderDetail.setSubtotal(item.getSubtotal());
            orderDetail.setShippingCost((float) Math.round(item.calculateFinalWeight() * item.getQuantity() * shippingRate.getRate() * 100) / 100);

            orderDetails.add(orderDetail);
        }

        cartService.deleteByCartId(cart.getId());

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toOrderDTO(savedOrder);
    }

    @Override
    public List<OrderDTO> getAllOrdersByCustomerEmail(String email, String sortField, String sortDir) throws NotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<Order> listOrders = orderRepository.findAllByUser(user, sort);
        List<OrderDTO> listOrderDTOs = listOrders.stream().map(orderMapper::toOrderDTO).collect(Collectors.toList());
        listOrderDTOs.forEach(this::setImagePathForOrderDetails);
        return listOrderDTOs;
    }

    @Override
    public Page<OrderDTO> getOrdersByCustomerEmailAndPage(String email, int pageNum, int pageSize, String sortField, String sortDir) throws NotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        Page<Order> page = orderRepository.findAllByUser(user, pageable);
        return page.map(orderMapper::toOrderDTO).map(this::setImagePathForOrderDetails);
    }

    @Override
    public OrderDTO setOrderReturnRequested(String email, Long id, OrderReturnRequest request) throws ConflictException, NotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        Order order = orderRepository.findByIdAndUserId(id, user.getId());
        if (order == null) {
            throw new NotFoundException("Could not find any order with ID " + id
                    + " belonging to the user has email " + email);
        }

        if (hasOrderTrack(order, OrderStatus.RETURN_REQUESTED) || hasOrderTrack(order, OrderStatus.RETURNED)) {
            throw new ConflictException("The order has been returned or a return request has been submitted");
        }

        OrderTrack track = new OrderTrack();
        track.setOrder(order);
        track.setUpdatedTime(new Date());
        track.setStatus(OrderStatus.RETURN_REQUESTED);

        String notes = "Reason: " + request.getReason();
        if (!"".equals(request.getNote())) {
            notes += ". " + request.getNote();
        }
        track.setNotes(notes);

        order.getOrderTrack().add(track);
        order.setStatus(OrderStatus.RETURN_REQUESTED);

        return orderMapper.toOrderDTO(orderRepository.save(order));
    }

    private boolean hasOrderTrack(Order order, OrderStatus orderStatus) {
        for (OrderTrack orderTrack: order.getOrderTrack()) {
            if (orderTrack.getStatus() == orderStatus) {
                return true;
            }
        }

        return false;
    }

    private float calculateShippingCost(Set<CartItem> items, ShippingRate shippingRate) {
        float shippingCostTotal = 0.0f;

        for (CartItem item : items) {
            shippingCostTotal += item.calculateFinalWeight() * item.getQuantity() * shippingRate.getRate();
        }

        return (float) Math.round(shippingCostTotal * 100) / 100;
    }

    private float calculateProductCost(Set<CartItem> items, ShippingRate shippingRate) {
        float productCostTotal = 0.0f;

        for (CartItem item : items) {
            productCostTotal += item.getProduct().getCost() * item.getQuantity();
        }

        return productCostTotal;
    }

    private Date getDeliverDate(Date from, int deliverDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);  // Set the date to the given date
        calendar.add(Calendar.DATE, deliverDays);  // Add the days

        return calendar.getTime();

    }
}
