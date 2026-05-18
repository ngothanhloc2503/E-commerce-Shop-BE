package com.store.ecommerce.service.impl;

import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.GetOrderInput;
import com.paypal.sdk.models.Order;
import com.paypal.sdk.models.OrderStatus;
import com.store.ecommerce.service.PaypalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalServiceImpl implements PaypalService {

    private final PaypalServerSdkClient paypalClient;

    @Override
    public boolean validateOrder(String orderId) throws BadRequestException {
        OrdersController ordersController = paypalClient.getOrdersController();

        try {
            ApiResponse<Order> response = ordersController.getOrder(
                    new GetOrderInput.Builder(orderId).build()
            );

            Order order = response.getResult();

            if (order == null || order.getId() == null) {
                throw new BadRequestException("Order not found");
            }

            String responseOrderId = order.getId();
            log.info("PayPal order validation - Request ID: {}, Response ID: {}, Status: {}",
                    orderId, responseOrderId, order.getStatus());

            if (!responseOrderId.equals(orderId)) {
                log.error("Order ID mismatch: expected={}, got={}", orderId, responseOrderId);
                throw new BadRequestException("Order ID does not match.");
            }

            OrderStatus status = order.getStatus();
            if (status == OrderStatus.APPROVED || status == OrderStatus.COMPLETED) {
                return true;
            }

            log.warn("Order {} has invalid status: {}", orderId, status);
            return false;

        } catch (ApiException e) {
            log.error("PayPal API error (code={}): {}", e.getResponseCode(), e.getMessage());
            int code = e.getResponseCode();

            String errorMessage = switch (code) {
                case 404 -> "Order ID not found: " + orderId;
                case 401 -> "PayPal authentication failed. Check API credentials.";
                case 400 -> "Bad request to PayPal: " + e.getMessage();
                case 500 -> "PayPal server error. Please try again later.";
                default -> "PayPal returned non-OK status code: " + code;
            };

            throw new BadRequestException(errorMessage);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during PayPal order validation for {}", orderId, e);
            throw new BadRequestException("Payment validation failed.");
        }
    }
}