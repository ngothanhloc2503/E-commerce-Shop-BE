package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.OrderDetail;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
public class OrderRepositoryTests {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testCreateNewOrderWithSingleProduct() {
        User user = entityManager.find(User.class, 31);
        Product product = entityManager.find(Product.class, 3);

        Order mainOrder = new Order();
        mainOrder.setUser(user);
        mainOrder.setFirstName(user.getFirstName());
        mainOrder.setLastName(user.getLastName());
        mainOrder.setPhoneNumber(user.getPhoneNumber());
        mainOrder.setAddressLine1(user.getAddressLine1());
        mainOrder.setAddressLine2(user.getAddressLine2());
        mainOrder.setCity(user.getCity());
        mainOrder.setCountry(user.getCountry());
        mainOrder.setState(user.getState());
        mainOrder.setPostalCode(user.getPostalCode());

        mainOrder.setShippingCost(10);
        mainOrder.setProductCost(product.getCost());
        mainOrder.setTax(0);
        mainOrder.setSubtotal(product.getPrice());
        mainOrder.setTotal(product.getPrice() + 30);

        mainOrder.setPaymentMethod(PaymentMethod.PAYPAL);
        mainOrder.setStatus(OrderStatus.DELIVERED);
        mainOrder.setOrderTime(new Date());
        mainOrder.setDeliverDate(new Date());
        mainOrder.setDeliverDays(2);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setProduct(product);
        orderDetail.setOrder(mainOrder);
        orderDetail.setProductCost(product.getCost());
        orderDetail.setShippingCost(30);
        orderDetail.setQuantity(1);
        orderDetail.setSubtotal(product.getPrice());
        orderDetail.setUnitPrice(product.getPrice());

        mainOrder.getOrderDetails().add(orderDetail);

        Order savedOrder = orderRepository.save(mainOrder);

        assertThat(savedOrder.getId()).isGreaterThan(0);
    }

    @Test
    public void testListOrders() {
        Iterable<Order> orders = orderRepository.findAll();
        orders.forEach(System.out::println);

        assertThat(orders).hasSizeGreaterThan(0);
    }
}
