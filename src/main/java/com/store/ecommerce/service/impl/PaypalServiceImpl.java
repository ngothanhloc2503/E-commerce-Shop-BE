package com.store.ecommerce.service.impl;

import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.models.Order;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.service.PaypalService;
import com.store.ecommerce.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaypalServiceImpl implements PaypalService {
    private final SettingService settingService;
    private final OrdersController ordersController;

    @Override
    public boolean validateOrder(String orderId) throws BadRequestException {
        try {
            // Get order details using PayPal SDK
            Order order = ordersController.getOrder(orderId);
            
            if (order == null || order.getId() == null) {
                throw new BadRequestException("Order not found");
            }
            
            String responseOrderId = order.getId();
            log.info("PayPal order validation - Request ID: {}, Response ID: {}", orderId, responseOrderId);
            
            return responseOrderId.equals(orderId);
            
        } catch (ApiException e) {
            log.error("PayPal API error during order validation", e);
            HttpStatus statusCode = HttpStatus.valueOf(e.getResponseCode());
            
            String errorMessage = switch (statusCode) {
                case NOT_FOUND -> "Order ID not found";
                case BAD_REQUEST -> "Bad Request to PayPal Checkout API";
                case INTERNAL_SERVER_ERROR -> "PayPal server error";
                default -> "PayPal returned non-OK status code: " + statusCode;
            };
            
            throw new BadRequestException(errorMessage);
        } catch (BadRequestException e) {
            log.error("Bad request during PayPal order validation", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during PayPal order validation", e);
            throw new BadRequestException("Payment validation failed.");
        }
    }
}
