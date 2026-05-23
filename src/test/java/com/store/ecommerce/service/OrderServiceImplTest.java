package com.store.ecommerce.service;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.OrderDetailDTO;
import com.store.ecommerce.dto.OrderTrackDTO;
import com.store.ecommerce.dto.request.OrderReturnRequest;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.OrderMapper;
import com.store.ecommerce.repository.*;
import com.store.ecommerce.service.impl.OrderServiceImpl;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private ShippingRateService shippingRateService;

    @Mock
    private CartService cartService;

    @Mock
    private AWSS3Service awsS3Service;

    @Mock
    private PagingAndSortingHelper pagingAndSortingHelper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;
    private OrderDTO sampleOrderDTO;
    private User sampleUser;
    private Cart sampleCart;
    private CartItem sampleCartItem;
    private Product sampleProduct;
    private Address sampleAddress;
    private ShippingRate sampleShippingRate;
    private Country sampleCountry;
    private State sampleState;

    @BeforeEach
    void setUp() {
        // Setup User
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("customer@example.com");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");

        // Setup Product
        sampleProduct = new Product();
        sampleProduct.setId(100L);
        sampleProduct.setName("Test Product");
        sampleProduct.setCost(50.0f);
        sampleProduct.setWeight(2.0f);
        sampleProduct.setPrice(80.0f);

        // Setup CartItem
        sampleCartItem = new CartItem();
        sampleCartItem.setId(10L);
        sampleCartItem.setProduct(sampleProduct);
        sampleCartItem.setQuantity(2);

        // Setup Cart
        sampleCart = new Cart();
        sampleCart.setId(1L);
        sampleCart.setItems(Set.of(sampleCartItem));
        sampleCart.setTotal(160.0f);

        // Setup Address
        sampleAddress = new Address();
        sampleAddress.setFirstName("John");
        sampleAddress.setLastName("Doe");
        sampleAddress.setPhoneNumber("0123456789");
        sampleAddress.setAddressLine1("123 Main St");
        sampleAddress.setAddressLine2("Apt 4");
        sampleAddress.setCity("New York");
        sampleAddress.setCountry("United States");
        sampleAddress.setState("New York");
        sampleAddress.setPostalCode("10001");

        // Setup ShippingRate
        sampleShippingRate = new ShippingRate();
        sampleShippingRate.setRate(5.0f);
        sampleShippingRate.setDays(3);

        // Setup Country
        sampleCountry = new Country();
        sampleCountry.setId(1L);
        sampleCountry.setName("United States");
        sampleCountry.setCode("US");

        // Setup State
        sampleState = new State();
        sampleState.setId(1L);
        sampleState.setName("New York");
        sampleState.setCountry(sampleCountry);

        // Setup Order
        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setUser(sampleUser);
        sampleOrder.setFirstName("John");
        sampleOrder.setLastName("Doe");
        sampleOrder.setPhoneNumber("0123456789");
        sampleOrder.setAddressLine1("123 Main St");
        sampleOrder.setAddressLine2("Apt 4");
        sampleOrder.setCity("New York");
        sampleOrder.setCountry("United States");
        sampleOrder.setState("New York");
        sampleOrder.setPostalCode("10001");
        sampleOrder.setShippingCost(10.0f);
        sampleOrder.setSubtotal(160.0f);
        sampleOrder.setTax(0.0f);
        sampleOrder.setTotal(170.0f);
        sampleOrder.setProductCost(100.0f);
        sampleOrder.setStatus(OrderStatus.NEW);
        sampleOrder.setPaymentMethod(PaymentMethod.COD);
        sampleOrder.setOrderTime(new Date());
        sampleOrder.setDeliverDays(3);
        sampleOrder.setDeliverDate(new Date());

        // Setup OrderDTO
        sampleOrderDTO = new OrderDTO();
        sampleOrderDTO.setId(1L);
        sampleOrderDTO.setUserId(1L);
        sampleOrderDTO.setFirstName("John");
        sampleOrderDTO.setLastName("Doe");
        sampleOrderDTO.setPhoneNumber("0123456789");
        sampleOrderDTO.setAddressLine1("123 Main St");
        sampleOrderDTO.setAddressLine2("Apt 4");
        sampleOrderDTO.setCity("New York");
        sampleOrderDTO.setCountry("United States");
        sampleOrderDTO.setState("New York");
        sampleOrderDTO.setPostalCode("10001");
        sampleOrderDTO.setShippingCost(10.0f);
        sampleOrderDTO.setSubtotal(160.0f);
        sampleOrderDTO.setTax(0.0f);
        sampleOrderDTO.setProductCost(100.0f);
        sampleOrderDTO.setStatus(OrderStatus.NEW);
        sampleOrderDTO.setPaymentMethod(PaymentMethod.COD);
        sampleOrderDTO.setOrderTime(new Date());
        sampleOrderDTO.setDeliverDays(3);
        sampleOrderDTO.setOrderDetails(new HashSet<>());
        sampleOrderDTO.setOrderTrack(new ArrayList<>());
    }

    // ============================= getAllOrders (no params) =============================

    @Nested
    @DisplayName("getAllOrders - Lấy tất cả đơn hàng")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return list of OrderDTO when orders exist")
        void shouldReturnListOfOrderDTOs_WhenOrdersExist() {
            Order order2 = new Order();
            order2.setId(2L);
            order2.setUser(sampleUser);

            OrderDTO dto2 = new OrderDTO();
            dto2.setId(2L);

            when(orderRepository.findAll()).thenReturn(List.of(sampleOrder, order2));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);
            when(orderMapper.toOrderDTO(order2)).thenReturn(dto2);

            List<OrderDTO> result = orderService.getAllOrders();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(1).getId()).isEqualTo(2L);
            verify(orderRepository).findAll();
            verify(orderMapper, times(2)).toOrderDTO(any(Order.class));
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void shouldReturnEmptyList_WhenNoOrdersExist() {
            when(orderRepository.findAll()).thenReturn(Collections.emptyList());

            List<OrderDTO> result = orderService.getAllOrders();

            assertThat(result).isEmpty();
            verify(orderRepository).findAll();
            verify(orderMapper, never()).toOrderDTO(any(Order.class));
        }
    }

    // ============================= getAllOrders (with keyword, sort) =============================

    @Nested
    @DisplayName("getAllOrders - Lấy đơn hàng với từ khóa và sắp xếp")
    class GetAllOrdersWithKeywordAndSortTests {

        @Test
        @DisplayName("Should return filtered and sorted orders ascending")
        void shouldReturnFilteredAndSortedOrders_Ascending() {
            Sort sort = Sort.by("orderTime").ascending();
            when(orderRepository.findAll(eq("john"), any(Sort.class))).thenReturn(List.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            List<OrderDTO> result = orderService.getAllOrders("john", "orderTime", "asc");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(orderRepository).findAll(eq("john"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return filtered and sorted orders descending")
        void shouldReturnFilteredAndSortedOrders_Descending() {
            when(orderRepository.findAll(eq("john"), any(Sort.class))).thenReturn(List.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            List<OrderDTO> result = orderService.getAllOrders("john", "orderTime", "desc");

            assertThat(result).hasSize(1);
            verify(orderRepository).findAll(eq("john"), any(Sort.class));
        }

        @Test
        @DisplayName("Should default to descending when sortDir is not 'asc'")
        void shouldDefaultToDescending_WhenSortDirIsNotAsc() {
            when(orderRepository.findAll(anyString(), any(Sort.class))).thenReturn(List.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            List<OrderDTO> result = orderService.getAllOrders("test", "total", "invalid");

            assertThat(result).hasSize(1);
            verify(orderRepository).findAll(eq("test"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return empty list when keyword matches no orders")
        void shouldReturnEmptyList_WhenKeywordMatchesNoOrders() {
            when(orderRepository.findAll(eq("nonexistent"), any(Sort.class))).thenReturn(Collections.emptyList());

            List<OrderDTO> result = orderService.getAllOrders("nonexistent", "orderTime", "asc");

            assertThat(result).isEmpty();
        }
    }

    // ============================= getOrdersByPage =============================

    @Nested
    @DisplayName("getOrdersByPage - Lấy đơn hàng phân trang")
    class GetOrdersByPageTests {

        @Test
        @DisplayName("Should return paginated OrderDTOs")
        @SuppressWarnings("unchecked")
        void shouldReturnPaginatedOrderDTOs() {
            Page<Order> orderPage = new PageImpl<>(List.of(sampleOrder));
            doReturn(orderPage).when(pagingAndSortingHelper).getPageEntities(any(OrderRepository.class));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            Page<OrderDTO> result = orderService.getOrdersByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
            verify(pagingAndSortingHelper).getPageEntities(orderRepository);
        }

        @Test
        @DisplayName("Should return empty page when no orders exist")
        @SuppressWarnings("unchecked")
        void shouldReturnEmptyPage_WhenNoOrdersExist() {
            Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
            doReturn(emptyPage).when(pagingAndSortingHelper).getPageEntities(any(OrderRepository.class));

            Page<OrderDTO> result = orderService.getOrdersByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ============================= getOrderById =============================

    @Nested
    @DisplayName("getOrderById - Lấy đơn hàng theo ID")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return OrderDTO when order exists")
        void shouldReturnOrderDTO_WhenOrderExists() {
            OrderDetailDTO detailDTO = new OrderDetailDTO();
            detailDTO.setProductId(100L);
            detailDTO.setProductImageName("image.png");

            sampleOrderDTO.setOrderDetails(Set.of(detailDTO));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("https://s3.aws/image.png");

            OrderDTO result = orderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(orderRepository).findById(1L);
            verify(awsS3Service).getImagePath(eq("product-images/100"), eq("image.png"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when order does not exist")
        void shouldThrowNotFoundException_WhenOrderDoesNotExist() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("999");

            verify(orderRepository).findById(999L);
            verify(orderMapper, never()).toOrderDTO(any(Order.class));
        }
    }

    // ============================= saveOrder =============================

    @Nested
    @DisplayName("saveOrder - Cập nhật đơn hàng")
    class SaveOrderTests {

        @Test
        @DisplayName("Should update and return OrderDTO when valid data provided")
        void shouldUpdateAndReturnOrderDTO_WhenValidDataProvided() {
            sampleOrder.setOrderDetails(new HashSet<>());
            sampleOrder.setOrderTrack(new ArrayList<>());

            OrderDetailDTO detailDTO = new OrderDetailDTO();
            detailDTO.setProductId(100L);
            detailDTO.setQuantity(2);
            detailDTO.setUnitPrice(80.0f);
            detailDTO.setProductCost(50.0f);
            detailDTO.setSubtotal(160.0f);
            detailDTO.setShippingCost(20.0f);

            OrderTrackDTO trackDTO = new OrderTrackDTO();
            trackDTO.setId(0L);
            trackDTO.setStatus(OrderStatus.NEW);
            trackDTO.setNotes("Order placed");
            trackDTO.setUpdatedTime(new Date());

            sampleOrderDTO.setOrderDetails(Set.of(detailDTO));
            sampleOrderDTO.setOrderTrack(List.of(trackDTO));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(countryRepository.findByNameIgnoreCase("United States")).thenReturn(Optional.of(sampleCountry));
            when(stateRepository.findByCountryAndNameIgnoreCase("United States", "New York")).thenReturn(Optional.of(sampleState));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            OrderDTO result = orderService.saveOrder(sampleOrderDTO);

            assertThat(result).isNotNull();
            verify(orderRepository).findById(1L);
            verify(countryRepository).findByNameIgnoreCase("United States");
            verify(stateRepository).findByCountryAndNameIgnoreCase("United States", "New York");
            verify(productRepository).findById(100L);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw NotFoundException when order does not exist")
        void shouldThrowNotFoundException_WhenOrderDoesNotExist() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());
            sampleOrderDTO.setId(999L);

            assertThatThrownBy(() -> orderService.saveOrder(sampleOrderDTO))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("999");

            verify(orderRepository).findById(999L);
            verify(countryRepository, never()).findByNameIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Should throw NotFoundException when country not found")
        void shouldThrowNotFoundException_WhenCountryNotFound() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(countryRepository.findByNameIgnoreCase("Unknown Country")).thenReturn(Optional.empty());
            sampleOrderDTO.setCountry("Unknown Country");

            assertThatThrownBy(() -> orderService.saveOrder(sampleOrderDTO))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Unknown Country");

            verify(countryRepository).findByNameIgnoreCase("Unknown Country");
            verify(stateRepository, never()).findByCountryAndNameIgnoreCase(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw NotFoundException when state not found in country")
        void shouldThrowNotFoundException_WhenStateNotFoundInCountry() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(countryRepository.findByNameIgnoreCase("United States")).thenReturn(Optional.of(sampleCountry));
            when(stateRepository.findByCountryAndNameIgnoreCase("United States", "Unknown State")).thenReturn(Optional.empty());
            sampleOrderDTO.setState("Unknown State");

            assertThatThrownBy(() -> orderService.saveOrder(sampleOrderDTO))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Unknown State");

            verify(stateRepository).findByCountryAndNameIgnoreCase("United States", "Unknown State");
        }

        @Test
        @DisplayName("Should throw ConflictException when userId does not match order's user")
        void shouldThrowConflictException_WhenUserIdDoesNotMatch() {
            User otherUser = new User();
            otherUser.setId(2L);
            sampleOrder.setUser(otherUser);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(countryRepository.findByNameIgnoreCase("United States")).thenReturn(Optional.of(sampleCountry));
            when(stateRepository.findByCountryAndNameIgnoreCase("United States", "New York")).thenReturn(Optional.of(sampleState));

            assertThatThrownBy(() -> orderService.saveOrder(sampleOrderDTO))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Could not change user of order");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw NotFoundException when product in order detail not found")
        void shouldThrowNotFoundException_WhenProductInOrderDetailNotFound() {
            sampleOrder.setOrderDetails(new HashSet<>());
            sampleOrder.setOrderTrack(new ArrayList<>());

            OrderDetailDTO detailDTO = new OrderDetailDTO();
            detailDTO.setProductId(999L);
            detailDTO.setQuantity(1);
            detailDTO.setUnitPrice(10.0f);
            detailDTO.setProductCost(5.0f);
            detailDTO.setSubtotal(10.0f);
            detailDTO.setShippingCost(2.0f);

            sampleOrderDTO.setOrderDetails(Set.of(detailDTO));
            sampleOrderDTO.setOrderTrack(new ArrayList<>());

            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(countryRepository.findByNameIgnoreCase("United States")).thenReturn(Optional.of(sampleCountry));
            when(stateRepository.findByCountryAndNameIgnoreCase("United States", "New York")).thenReturn(Optional.of(sampleState));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.saveOrder(sampleOrderDTO))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("999");

            verify(productRepository).findById(999L);
        }

        @Test
        @DisplayName("Should calculate total correctly when saving order")
        void shouldCalculateTotalCorrectly_WhenSavingOrder() {
            sampleOrder.setOrderDetails(new HashSet<>());
            sampleOrder.setOrderTrack(new ArrayList<>());

            sampleOrderDTO.setShippingCost(10.0f);
            sampleOrderDTO.setSubtotal(200.0f);
            sampleOrderDTO.setTax(5.0f);
            sampleOrderDTO.setOrderDetails(new HashSet<>());
            sampleOrderDTO.setOrderTrack(new ArrayList<>());

            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(countryRepository.findByNameIgnoreCase("United States")).thenReturn(Optional.of(sampleCountry));
            when(stateRepository.findByCountryAndNameIgnoreCase("United States", "New York")).thenReturn(Optional.of(sampleState));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            orderService.saveOrder(sampleOrderDTO);

            verify(orderRepository).save(argThat(order ->
                    order.getTotal() == 215.0f  // shippingCost + subtotal + tax = 10 + 200 + 5
            ));
        }
    }

    // ============================= deleteById =============================

    @Nested
    @DisplayName("deleteById - Xóa đơn hàng theo ID")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should delete order when order exists")
        void shouldDeleteOrder_WhenOrderExists() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            orderService.deleteById(1L);

            verify(orderRepository).findById(1L);
            verify(orderRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw NotFoundException when order does not exist")
        void shouldThrowNotFoundException_WhenOrderDoesNotExist() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.deleteById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("999");

            verify(orderRepository).findById(999L);
            verify(orderRepository, never()).deleteById(anyLong());
        }
    }

    // ============================= createOrder =============================

    @Nested
    @DisplayName("createOrder - Tạo đơn hàng mới từ giỏ hàng")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order with COD payment method successfully")
        void shouldCreateOrderWithCOD_Successfully() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(sampleCart);
            when(addressService.getDefaultAddress("customer@example.com")).thenReturn(sampleAddress);
            when(shippingRateService.getShippingRateByCountryAndState("United States", "New York"))
                    .thenReturn(sampleShippingRate);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            OrderDTO result = orderService.createOrder("customer@example.com", PaymentMethod.COD);

            assertThat(result).isNotNull();
            verify(userRepository).findByEmail("customer@example.com");
            verify(cartRepository).findByUserEmail("customer@example.com");
            verify(addressService).getDefaultAddress("customer@example.com");
            verify(shippingRateService).getShippingRateByCountryAndState("United States", "New York");
            verify(cartService).deleteByCartId(sampleCart.getId());
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should create order with PAYPAL payment method and set status to PAID")
        void shouldCreateOrderWithPAYPAL_AndSetStatusToPaid() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(sampleCart);
            when(addressService.getDefaultAddress("customer@example.com")).thenReturn(sampleAddress);
            when(shippingRateService.getShippingRateByCountryAndState("United States", "New York"))
                    .thenReturn(sampleShippingRate);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            orderService.createOrder("customer@example.com", PaymentMethod.PAYPAL);

            verify(orderRepository).save(argThat(order -> {
                // Should have status PAID
                if (order.getStatus() != OrderStatus.PAID) return false;
                // Should have 2 tracks: NEW and PAID
                return order.getOrderTrack().size() == 2;
            }));
        }

        @Test
        @DisplayName("Should create order with COD and set status to NEW")
        void shouldCreateOrderWithCOD_AndSetStatusToNew() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(sampleCart);
            when(addressService.getDefaultAddress("customer@example.com")).thenReturn(sampleAddress);
            when(shippingRateService.getShippingRateByCountryAndState("United States", "New York"))
                    .thenReturn(sampleShippingRate);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            orderService.createOrder("customer@example.com", PaymentMethod.COD);

            verify(orderRepository).save(argThat(order -> {
                if (order.getStatus() != OrderStatus.NEW) return false;
                // Should have only 1 track: NEW
                return order.getOrderTrack().size() == 1;
            }));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user email not found")
        void shouldThrowNotFoundException_WhenUserEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder("unknown@example.com", PaymentMethod.COD))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("unknown@example.com");

            verify(cartRepository, never()).findByUserEmail(anyString());
        }

        @Test
        @DisplayName("Should throw NotFoundException when cart is null")
        void shouldThrowNotFoundException_WhenCartIsNull() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(null);

            assertThatThrownBy(() -> orderService.createOrder("customer@example.com", PaymentMethod.COD))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No item found");

            verify(addressService, never()).getDefaultAddress(anyString());
        }

        @Test
        @DisplayName("Should throw NotFoundException when shipping rate is null for default address")
        void shouldThrowNotFoundException_WhenShippingRateIsNull() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(sampleCart);
            when(addressService.getDefaultAddress("customer@example.com")).thenReturn(sampleAddress);
            when(shippingRateService.getShippingRateByCountryAndState("United States", "New York"))
                    .thenReturn(null);

            assertThatThrownBy(() -> orderService.createOrder("customer@example.com", PaymentMethod.COD))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Default address does not supported for shipping");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should create order details from cart items")
        void shouldCreateOrderDetailsFromCartItems() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(sampleCart);
            when(addressService.getDefaultAddress("customer@example.com")).thenReturn(sampleAddress);
            when(shippingRateService.getShippingRateByCountryAndState("United States", "New York"))
                    .thenReturn(sampleShippingRate);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            orderService.createOrder("customer@example.com", PaymentMethod.COD);

            verify(orderRepository).save(argThat(order -> {
                if (order.getOrderDetails().size() != 1) return false;
                OrderDetail detail = order.getOrderDetails().iterator().next();
                return detail.getProduct().getId().equals(100L)
                        && detail.getQuantity() == 2
                        && detail.getUnitPrice() == 80.0f;
            }));
        }

        @Test
        @DisplayName("Should delete cart after creating order")
        void shouldDeleteCart_AfterCreatingOrder() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(cartRepository.findByUserEmail("customer@example.com")).thenReturn(sampleCart);
            when(addressService.getDefaultAddress("customer@example.com")).thenReturn(sampleAddress);
            when(shippingRateService.getShippingRateByCountryAndState("United States", "New York"))
                    .thenReturn(sampleShippingRate);
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(sampleOrderDTO);

            orderService.createOrder("customer@example.com", PaymentMethod.COD);

            verify(cartService).deleteByCartId(sampleCart.getId());
        }
    }

    // ============================= getAllOrdersByCustomerEmail =============================

    @Nested
    @DisplayName("getAllOrdersByCustomerEmail - Lấy đơn hàng theo email khách hàng")
    class GetAllOrdersByCustomerEmailTests {

        @Test
        @DisplayName("Should return list of OrderDTOs for customer ascending")
        void shouldReturnListOfOrderDTOs_ForCustomer_Ascending() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Sort.class))).thenReturn(List.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            List<OrderDTO> result = orderService.getAllOrdersByCustomerEmail("customer@example.com", "orderTime", "asc");

            assertThat(result).hasSize(1);
            verify(orderRepository).findAllByUser(eq(sampleUser), any(Sort.class));
        }

        @Test
        @DisplayName("Should return list of OrderDTOs for customer descending")
        void shouldReturnListOfOrderDTOs_ForCustomer_Descending() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Sort.class))).thenReturn(List.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            List<OrderDTO> result = orderService.getAllOrdersByCustomerEmail("customer@example.com", "orderTime", "desc");

            assertThat(result).hasSize(1);
            verify(orderRepository).findAllByUser(eq(sampleUser), any(Sort.class));
        }

        @Test
        @DisplayName("Should set image path for order details in each DTO")
        void shouldSetImagePath_ForOrderDetailsInEachDTO() {
            OrderDetailDTO detailDTO = new OrderDetailDTO();
            detailDTO.setProductId(100L);
            detailDTO.setProductImageName("image.png");
            sampleOrderDTO.setOrderDetails(Set.of(detailDTO));

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Sort.class))).thenReturn(List.of(sampleOrder));
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("https://s3.aws/image.png");

            List<OrderDTO> result = orderService.getAllOrdersByCustomerEmail("customer@example.com", "orderTime", "asc");

            assertThat(result).hasSize(1);
            verify(awsS3Service).getImagePath(eq("product-images/100"), eq("image.png"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user email not found")
        void shouldThrowNotFoundException_WhenUserEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getAllOrdersByCustomerEmail("unknown@example.com", "orderTime", "asc"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("unknown@example.com");

            verify(orderRepository, never()).findAllByUser(any(User.class), any(Sort.class));
        }

        @Test
        @DisplayName("Should return empty list when customer has no orders")
        void shouldReturnEmptyList_WhenCustomerHasNoOrders() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Sort.class))).thenReturn(Collections.emptyList());

            List<OrderDTO> result = orderService.getAllOrdersByCustomerEmail("customer@example.com", "orderTime", "asc");

            assertThat(result).isEmpty();
        }
    }

    // ============================= getOrdersByCustomerEmailAndPage =============================

    @Nested
    @DisplayName("getOrdersByCustomerEmailAndPage - Lấy đơn hàng phân trang theo email khách hàng")
    class GetOrdersByCustomerEmailAndPageTests {

        @Test
        @DisplayName("Should return paginated OrderDTOs for customer ascending")
        void shouldReturnPaginatedOrderDTOs_ForCustomer_Ascending() {
            Page<Order> orderPage = new PageImpl<>(List.of(sampleOrder));

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Pageable.class))).thenReturn(orderPage);
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            Page<OrderDTO> result = orderService.getOrdersByCustomerEmailAndPage(
                    "customer@example.com", 1, 10, "orderTime", "asc");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return paginated OrderDTOs for customer descending")
        void shouldReturnPaginatedOrderDTOs_ForCustomer_Descending() {
            Page<Order> orderPage = new PageImpl<>(List.of(sampleOrder));

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Pageable.class))).thenReturn(orderPage);
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);

            Page<OrderDTO> result = orderService.getOrdersByCustomerEmailAndPage(
                    "customer@example.com", 1, 10, "orderTime", "desc");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should set image path for order details in paginated results")
        void shouldSetImagePath_ForOrderDetailsInPaginatedResults() {
            OrderDetailDTO detailDTO = new OrderDetailDTO();
            detailDTO.setProductId(100L);
            detailDTO.setProductImageName("image.png");
            sampleOrderDTO.setOrderDetails(Set.of(detailDTO));

            Page<Order> orderPage = new PageImpl<>(List.of(sampleOrder));

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Pageable.class))).thenReturn(orderPage);
            when(orderMapper.toOrderDTO(sampleOrder)).thenReturn(sampleOrderDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("https://s3.aws/image.png");

            Page<OrderDTO> result = orderService.getOrdersByCustomerEmailAndPage(
                    "customer@example.com", 1, 10, "orderTime", "asc");

            assertThat(result).isNotNull();
            verify(awsS3Service).getImagePath(eq("product-images/100"), eq("image.png"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user email not found")
        void shouldThrowNotFoundException_WhenUserEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrdersByCustomerEmailAndPage(
                    "unknown@example.com", 1, 10, "orderTime", "asc"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("unknown@example.com");

            verify(orderRepository, never()).findAllByUser(any(User.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when customer has no orders")
        void shouldReturnEmptyPage_WhenCustomerHasNoOrders() {
            Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findAllByUser(eq(sampleUser), any(Pageable.class))).thenReturn(emptyPage);

            Page<OrderDTO> result = orderService.getOrdersByCustomerEmailAndPage(
                    "customer@example.com", 1, 10, "orderTime", "asc");

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ============================= setOrderReturnRequested =============================

    @Nested
    @DisplayName("setOrderReturnRequested - Yêu cầu trả hàng")
    class SetOrderReturnRequestedTests {

        private OrderReturnRequest returnRequest;

        @BeforeEach
        void setUpReturnRequest() {
            returnRequest = new OrderReturnRequest();
            returnRequest.setReason("Product damaged");
            returnRequest.setNote("Item arrived broken");
        }

        @Test
        @DisplayName("Should set return requested successfully")
        void shouldSetReturnRequested_Successfully() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(sampleUser);
            order.setStatus(OrderStatus.DELIVERED);
            order.setOrderTrack(new ArrayList<>());
            order.setOrderDetails(new HashSet<>());

            OrderTrack deliveredTrack = new OrderTrack();
            deliveredTrack.setStatus(OrderStatus.DELIVERED);
            order.getOrderTrack().add(deliveredTrack);

            OrderDTO returnDTO = new OrderDTO();
            returnDTO.setId(1L);
            returnDTO.setStatus(OrderStatus.RETURN_REQUESTED);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(order);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(returnDTO);

            OrderDTO result = orderService.setOrderReturnRequested("customer@example.com", 1L, returnRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
            verify(orderRepository).save(argThat(o ->
                    o.getStatus() == OrderStatus.RETURN_REQUESTED
            ));
        }

        @Test
        @DisplayName("Should include reason and note in track notes")
        void shouldIncludeReasonAndNoteInTrackNotes() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(sampleUser);
            order.setStatus(OrderStatus.DELIVERED);
            order.setOrderTrack(new ArrayList<>());
            order.setOrderDetails(new HashSet<>());

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(order);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(new OrderDTO());

            orderService.setOrderReturnRequested("customer@example.com", 1L, returnRequest);

            verify(orderRepository).save(argThat(o -> {
                OrderTrack lastTrack = o.getOrderTrack().get(o.getOrderTrack().size() - 1);
                return lastTrack.getNotes().contains("Product damaged")
                        && lastTrack.getNotes().contains("Item arrived broken");
            }));
        }

        @Test
        @DisplayName("Should include only reason in track notes when note is empty")
        void shouldIncludeOnlyReasonInTrackNotes_WhenNoteIsEmpty() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(sampleUser);
            order.setStatus(OrderStatus.DELIVERED);
            order.setOrderTrack(new ArrayList<>());
            order.setOrderDetails(new HashSet<>());

            returnRequest.setNote("");

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(order);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(new OrderDTO());

            orderService.setOrderReturnRequested("customer@example.com", 1L, returnRequest);

            verify(orderRepository).save(argThat(o -> {
                OrderTrack lastTrack = o.getOrderTrack().get(o.getOrderTrack().size() - 1);
                return lastTrack.getNotes().contains("Product damaged")
                        && !lastTrack.getNotes().contains(". ");
            }));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user email not found")
        void shouldThrowNotFoundException_WhenUserEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.setOrderReturnRequested("unknown@example.com", 1L, returnRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("unknown@example.com");

            verify(orderRepository, never()).findByIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw NotFoundException when order not found for user")
        void shouldThrowNotFoundException_WhenOrderNotFoundForUser() {
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.setOrderReturnRequested("customer@example.com", 999L, returnRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("999");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when return already requested")
        void shouldThrowConflictException_WhenReturnAlreadyRequested() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(sampleUser);
            order.setStatus(OrderStatus.RETURN_REQUESTED);
            order.setOrderTrack(new ArrayList<>());
            order.setOrderDetails(new HashSet<>());

            OrderTrack returnTrack = new OrderTrack();
            returnTrack.setStatus(OrderStatus.RETURN_REQUESTED);
            order.getOrderTrack().add(returnTrack);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.setOrderReturnRequested("customer@example.com", 1L, returnRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("returned or a return request has been submitted");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when order already returned")
        void shouldThrowConflictException_WhenOrderAlreadyReturned() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(sampleUser);
            order.setStatus(OrderStatus.RETURNED);
            order.setOrderTrack(new ArrayList<>());
            order.setOrderDetails(new HashSet<>());

            OrderTrack returnedTrack = new OrderTrack();
            returnedTrack.setStatus(OrderStatus.RETURNED);
            order.getOrderTrack().add(returnedTrack);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.setOrderReturnRequested("customer@example.com", 1L, returnRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("returned or a return request has been submitted");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should add RETURN_REQUESTED track with correct status")
        void shouldAddReturnRequestedTrack_WithCorrectStatus() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(sampleUser);
            order.setStatus(OrderStatus.DELIVERED);
            order.setOrderTrack(new ArrayList<>());
            order.setOrderDetails(new HashSet<>());

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(order);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(new OrderDTO());

            orderService.setOrderReturnRequested("customer@example.com", 1L, returnRequest);

            verify(orderRepository).save(argThat(o -> {
                OrderTrack lastTrack = o.getOrderTrack().get(o.getOrderTrack().size() - 1);
                return lastTrack.getStatus() == OrderStatus.RETURN_REQUESTED
                        && lastTrack.getUpdatedTime() != null;
            }));
        }
    }
}