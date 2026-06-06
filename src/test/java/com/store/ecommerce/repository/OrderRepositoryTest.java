package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("OrderRepository Integration Tests")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private User john;
    private User jane;
    private User bob;

    @BeforeEach
    void setUp() {
        // Persist users first (Order depends on User)
        john = persistUser("john@example.com", "John", "Doe");
        jane = persistUser("jane@example.com", "Jane", "Smith");
        bob = persistUser("bob@example.com", "Bob", "Wilson");

        // John's orders
        persistOrder(john, "John", "Doe", "123 Main St", "Apt 4",
                "New York", "NY", "USA", "CREDIT_CARD", "DELIVERED",
                100.0f, 20.0f, 120.0f, daysAgo(10));
        persistOrder(john, "John", "Doe", "456 Oak Ave", null,
                "Los Angeles", "CA", "USA", "PAYPAL", "SHIPPING",
                200.0f, 30.0f, 230.0f, daysAgo(5));
        persistOrder(john, "John", "Doe", "789 Pine Rd", "Suite 100",
                "Chicago", "IL", "USA", "CREDIT_CARD", "PROCESSING",
                150.0f, 25.0f, 175.0f, daysAgo(1));

        // Jane's orders
        persistOrder(jane, "Jane", "Smith", "321 Elm St", null,
                "Houston", "TX", "USA", "COD", "DELIVERED",
                300.0f, 50.0f, 350.0f, daysAgo(7));
        persistOrder(jane, "Jane", "Smith", "654 Maple Dr", "Unit B",
                "Phoenix", "AZ", "USA", "CREDIT_CARD", "NEW",
                80.0f, 10.0f, 90.0f, daysAgo(0));

        // Bob's order
        persistOrder(bob, "Bob", "Wilson", "987 Cedar Ln", null,
                "Seattle", "WA", "USA", "PAYPAL", "SHIPPING",
                500.0f, 60.0f, 560.0f, daysAgo(3));
    }

    // ======================== FIND ALL (LIST) ========================

    @Test
    @DisplayName("Should return all orders as list")
    void findAll_AsList() {
        // Act
        List<Order> orders = orderRepository.findAll();

        // Assert — 6 orders from setUp
        assertThat(orders).hasSize(6);
    }

    // ======================== FIND ALL BY KEYWORD (PAGEABLE) ========================

    @Test
    @DisplayName("Should find orders by keyword matching user firstName")
    void findAll_KeywordPageable_MatchUserFirstName() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("John", pageable);

        // Assert — John's 3 orders
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(o -> o.getUser().getFirstName().equals("John"));
    }

    @Test
    @DisplayName("Should find orders by keyword matching user lastName")
    void findAll_KeywordPageable_MatchUserLastName() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("Smith", pageable);

        // Assert — Jane's 2 orders
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(o -> o.getUser().getLastName().equals("Smith"));
    }

    @Test
    @DisplayName("Should find orders by keyword matching status")
    void findAll_KeywordPageable_MatchStatus() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("DELIVERED", pageable);

        // Assert — 2 DELIVERED orders
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(o -> o.getStatus().equals(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Should find orders by keyword matching city")
    void findAll_KeywordPageable_MatchCity() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("Los Angeles", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCity()).isEqualTo("Los Angeles");
    }

    @Test
    @DisplayName("Should find orders by keyword matching payment method")
    void findAll_KeywordPageable_MatchPaymentMethod() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("PAYPAL", pageable);

        // Assert — 2 PAYPAL orders
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(o -> o.getPaymentMethod().equals(PaymentMethod.PAYPAL));
    }

    @Test
    @DisplayName("Should find orders by keyword matching order id")
    void findAll_KeywordPageable_MatchOrderId() {
        // Arrange
        List<Order> allOrders = orderRepository.findAll();
        Long targetId = allOrders.get(0).getId();
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword(String.valueOf(targetId), pageable);

        // Assert — should find order by id
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).anyMatch(o -> o.getId().equals(targetId));
    }

    @Test
    @DisplayName("Should find orders by keyword matching country")
    void findAll_KeywordPageable_MatchCountry() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("USA", pageable);

        // Assert — all orders are in USA
        assertThat(result.getContent()).hasSize(6);
    }

    @Test
    @DisplayName("Should find orders by keyword matching state")
    void findAll_KeywordPageable_MatchState() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Order> result = orderRepository.searchByKeyword("TX", pageable);

        // Assert — 1 TX order
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getState()).isEqualTo("TX");
    }

    @Test
    @DisplayName("Should return paginated keyword search results")
    void findAll_KeywordPageable_Pagination() {
        // Arrange — "USA" matches all 6 orders, page size 2
        PageRequest pageable = PageRequest.of(0, 2);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("USA", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return empty page when keyword matches nothing")
    void findAll_KeywordPageable_NoMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.searchByKeyword("XYZNonExistent", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ======================== FIND ALL BY KEYWORD (SORT) ========================

    @Test
    @DisplayName("Should find orders by keyword with sort ascending")
    void findAll_KeywordSort_Ascending() {
        // Arrange
        Sort sort = Sort.by("id").ascending();

        // Act
        List<Order> result = orderRepository.searchByKeyword("USA", sort);

        // Assert — all 6 orders, sorted by id ascending
        assertThat(result).hasSize(6);
        List<Long> ids = result.stream().map(Order::getId).toList();
        assertThat(ids).isSorted();
    }

    @Test
    @DisplayName("Should find orders by keyword with sort descending")
    void findAll_KeywordSort_Descending() {
        // Arrange
        Sort sort = Sort.by("id").descending();

        // Act
        List<Order> result = orderRepository.searchByKeyword("USA", sort);

        // Assert
        assertThat(result).hasSize(6);
        List<Long> ids = result.stream().map(Order::getId).toList();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    @DisplayName("Should find orders by keyword matching status with sort")
    void findAll_KeywordSort_MatchStatus() {
        // Arrange
        Sort sort = Sort.by("id").ascending();

        // Act
        List<Order> result = orderRepository.searchByKeyword("SHIPPING", sort);

        // Assert — 2 SHIPPING orders
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> o.getStatus().equals(OrderStatus.SHIPPING));
    }

    @Test
    @DisplayName("Should return empty list when keyword matches nothing with sort")
    void findAll_KeywordSort_NoMatch() {
        // Arrange
        Sort sort = Sort.by("id").ascending();

        // Act
        List<Order> result = orderRepository.searchByKeyword("XYZNonExistent", sort);

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== FIND BY ORDER TIME BETWEEN ========================

    @Test
    @DisplayName("Should find orders within date range")
    void findByOrderTimeBetween_WithinRange() {
        // Arrange — search between 7 days ago and 2 days ago
        Date startTime = daysAgo(8);
        Date endTime = daysAgo(2);

        // Act
        List<Order> result = orderRepository.findByOrderTimeBetween(startTime, endTime);

        // Assert — orders from: Jane (7d), Bob (3d), John (5d)
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(o ->
                o.getOrderTime().compareTo(startTime) >= 0 &&
                        o.getOrderTime().compareTo(endTime) <= 0
        );
    }

    @Test
    @DisplayName("Should find all orders with wide date range")
    void findByOrderTimeBetween_WideRange() {
        // Arrange — search from 30 days ago to tomorrow
        Date startTime = daysAgo(30);
        Date endTime = daysAgo(-1);

        // Act
        List<Order> result = orderRepository.findByOrderTimeBetween(startTime, endTime);

        // Assert — all 6 orders
        assertThat(result).hasSize(6);
    }

    @Test
    @DisplayName("Should return empty list when no orders in date range")
    void findByOrderTimeBetween_NoOrdersInRange() {
        // Arrange — search in the far future
        Date startTime = daysAgo(-100); // 100 days in the future
        Date endTime = daysAgo(-50);

        // Act
        List<Order> result = orderRepository.findByOrderTimeBetween(startTime, endTime);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find single order with narrow date range")
    void findByOrderTimeBetween_NarrowRange() {
        Date startTime = daysAgo(4);
        Date endTime = daysAgo(2);

        List<Order> result = orderRepository.findByOrderTimeBetween(startTime, endTime);

        assertThat(result).hasSize(1);
        Order found = entityManager.find(Order.class, result.get(0).getId());
        assertThat(found.getUser()).isNotNull();
        assertThat(found.getUser().getFirstName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("Should return results ordered by orderTime ascending")
    void findByOrderTimeBetween_OrderedByTime() {
        // Arrange
        Date startTime = daysAgo(30);
        Date endTime = daysAgo(-1);

        // Act
        List<Order> result = orderRepository.findByOrderTimeBetween(startTime, endTime);

        // Assert — verify sorted by orderTime ASC
        assertThat(result).hasSize(6);
        List<Date> times = result.stream().map(Order::getOrderTime).toList();
        assertThat(times).isSorted();
    }

    // ======================== FIND ALL BY USER (PAGEABLE) ========================

    @Test
    @DisplayName("Should find orders by user with pagination")
    void findAllByUser_Pageable() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 2);

        // Act
        Page<Order> result = orderRepository.findAllByUser(john, pageable);

        // Assert — John has 3 orders, page size 2
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(o -> o.getUser().getId().equals(john.getId()));
    }

    @Test
    @DisplayName("Should return second page of user orders")
    void findAllByUser_Pageable_SecondPage() {
        // Arrange
        PageRequest pageable = PageRequest.of(1, 2);

        // Act
        Page<Order> result = orderRepository.findAllByUser(john, pageable);

        // Assert — 3 total - 2 from page 1
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should find Jane's orders with pagination")
    void findAllByUser_Pageable_Jane() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.findAllByUser(jane, pageable);

        // Assert — Jane has 2 orders
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(o -> o.getUser().getId().equals(jane.getId()));
    }

    @Test
    @DisplayName("Should return empty page for user with no orders")
    void findAllByUser_Pageable_NoOrders() {
        // Arrange
        User newUser = persistUser("empty@example.com", "Empty", "User");
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Order> result = orderRepository.findAllByUser(newUser, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ======================== FIND ALL BY USER (SORT) ========================

    @Test
    @DisplayName("Should find orders by user with sort")
    void findAllByUser_Sort() {
        // Arrange
        Sort sort = Sort.by("id").ascending();

        // Act
        List<Order> result = orderRepository.findAllByUser(john, sort);

        // Assert — John's 3 orders
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(o -> o.getUser().getId().equals(john.getId()));
    }

    @Test
    @DisplayName("Should find Jane's orders with sort")
    void findAllByUser_Sort_Jane() {
        // Arrange
        Sort sort = Sort.by("id").ascending();

        // Act
        List<Order> result = orderRepository.findAllByUser(jane, sort);

        // Assert — Jane's 2 orders
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list for user with no orders with sort")
    void findAllByUser_Sort_NoOrders() {
        // Arrange
        User newUser = persistUser("empty2@example.com", "Empty", "User");
        Sort sort = Sort.by("id").ascending();

        // Act
        List<Order> result = orderRepository.findAllByUser(newUser, sort);

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== FIND BY ID AND USER ID ========================

    @Test
    @DisplayName("Should find order by id and user id")
    void findByIdAndUserId_Found() {
        // Arrange — get one of John's orders
        List<Order> johnOrders = orderRepository.findAllByUser(john, Sort.by("id").ascending());
        Long orderId = johnOrders.get(0).getId();

        // Act
        Optional<Order> result = orderRepository.findByIdAndUserId(orderId, john.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(orderId);
        assertThat(result.get().getUser().getId()).isEqualTo(john.getId());
    }

    @Test
    @DisplayName("Should return null when order id does not belong to user")
    void findByIdAndUserId_WrongUser() {
        // Arrange — get one of John's orders, but search with Jane's userId
        List<Order> johnOrders = orderRepository.findAllByUser(john, Sort.by("id").ascending());
        Long orderId = johnOrders.get(0).getId();

        // Act
        Optional<Order> result = orderRepository.findByIdAndUserId(orderId, jane.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return null when order id does not exist")
    void findByIdAndUserId_OrderNotFound() {
        // Act
        Optional<Order> result = orderRepository.findByIdAndUserId(99999L, john.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve order")
    void save_AndFindById() {
        // Arrange
        Order order = createOrder(john, "John", "Doe", "555 Test St", null,
                "Boston", "MA", "USA", "CREDIT_CARD", "NEW",
                50.0f, 5.0f, 55.0f, new Date());

        // Act
        Order saved = orderRepository.save(order);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getUser().getId()).isEqualTo(john.getId());
    }

    @Test
    @DisplayName("Should delete order by id")
    void deleteById_Success() {
        // Arrange
        List<Order> allOrders = orderRepository.findAll();
        Long orderId = allOrders.get(0).getId();

        // Act
        orderRepository.deleteById(orderId);

        // Assert
        assertThat(orderRepository.findById(orderId)).isEmpty();
    }

    @Test
    @DisplayName("Should count orders correctly")
    void count_Success() {
        // 6 from setUp
        assertThat(orderRepository.count()).isEqualTo(6);
    }

    // ======================== HELPER METHODS ========================

    private User persistUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setAuthenticationType(AuthenticationType.DATABASE);
        user.setAddressLine1("123 Default St");
        user.setCity("Default City");
        user.setState("DS");
        user.setPostalCode("00000");
        user.setCountry("US");
        user.setPhoneNumber("000-000-0000");
        user.setBirthOfDate(DateUtil.toDateTime(LocalDate.of(1990, 1, 1)));
        return entityManager.persistAndFlush(user);
    }

    private void persistOrder(User user, String firstName, String lastName,
                              String addressLine1, String addressLine2,
                              String city, String state, String country,
                              String paymentMethod, String status,
                              float productCost, float shippingCost, float total,
                              Date orderTime) {
        Order order = createOrder(user, firstName, lastName, addressLine1, addressLine2,
                city, state, country, paymentMethod, status,
                productCost, shippingCost, total, orderTime);
        entityManager.persistAndFlush(order);
    }

    private Order createOrder(User user, String firstName, String lastName,
                              String addressLine1, String addressLine2,
                              String city, String state, String country,
                              String paymentMethod, String status,
                              float productCost, float shippingCost, float total,
                              Date orderTime) {
        Order order = new Order();
        order.setUser(user);
        order.setFirstName(firstName);
        order.setLastName(lastName);
        order.setAddressLine1(addressLine1);
        order.setAddressLine2(addressLine2);
        order.setCity(city);
        order.setState(state);
        order.setCountry(country);
        order.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        order.setProductCost(productCost);
        order.setShippingCost(shippingCost);
        order.setSubtotal(productCost);
        order.setTotal(total);
        order.setOrderTime(orderTime);
        order.setPhoneNumber("000-000-0000");
        order.setPostalCode("00000");
        order.setDeliverDays(3);
        order.setTax(0.0f);
        return order;
    }

    private Date daysAgo(int days) {
        long millis = System.currentTimeMillis() - ((long) days * 24 * 60 * 60 * 1000);
        return new Date(millis);
    }
}