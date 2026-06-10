package com.store.ecommerce.controller;

import com.store.ecommerce.dto.CheckoutInfo;
import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.request.PayPalCheckoutRequest;
import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.wrapper.CheckoutInfoWrapper;
import com.store.ecommerce.dto.wrapper.OrderWrapper;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.service.CheckoutService;
import com.store.ecommerce.service.OrderService;
import com.store.ecommerce.service.PaypalService;
import com.store.ecommerce.service.SettingService;
import com.store.ecommerce.util.MailUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "APIs for checkout and order processing")
public class CheckoutController {
    private final OrderService orderService;
    private final CheckoutService checkoutService;
    private final SettingService settingService;
    private final PaypalService paypalService;

    @Operation(
            summary = "Get checkout information",
            description = "Retrieve checkout data including cart items, totals, and shipping info"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Checkout information retrieved successfully",
            content = @Content(schema = @Schema(implementation = CheckoutInfoWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<CheckoutInfo>> getCheckoutInformation(
            Authentication authentication) {

        CheckoutInfo data = checkoutService.getCheckoutInformation(authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<CheckoutInfo>builder()
                        .success(true)
                        .message("Checkout information retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Place order",
            description = "Create an order using selected payment method (COD, PAYPAL, etc.)"
    )
    @Parameter(
            name = "paymentMethod",
            description = "Payment method (e.g., COD, PAYPAL)",
            required = true,
            example = "COD"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order placed successfully",
                    content = @Content(schema = @Schema(implementation = OrderWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment method",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("")
    public ResponseEntity<ApiSuccessResponse<OrderDTO>> checkout(
            Authentication authentication,
            @RequestParam("paymentMethod") String paymentType) {

        PaymentMethod paymentMethod;

        try {
            paymentMethod = PaymentMethod.valueOf(paymentType.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payment method");
        }

        String email = authentication.getName();
        OrderDTO order = orderService.createOrder(email, paymentMethod);

        sendOrderConfirmationEmail(email, order);

        return ResponseEntity.ok(
                ApiSuccessResponse.<OrderDTO>builder()
                        .success(true)
                        .message("Order placed successfully")
                        .data(order)
                        .build()
        );
    }

    @Operation(
            summary = "Checkout with PayPal",
            description = "Validate PayPal order and complete checkout"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order placed successfully",
                    content = @Content(schema = @Schema(implementation = OrderWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid PayPal order",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/paypal")
    public ResponseEntity<ApiSuccessResponse<OrderDTO>> processPayPalCheckout(
            Authentication authentication,
            @RequestBody PayPalCheckoutRequest request) {

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }

        try {
            boolean isValid = paypalService.validateOrder(request.getOrderId());

            if (!isValid) {
                throw new IllegalArgumentException("Invalid PayPal order");
            }

        } catch (BadRequestException e) {
            throw new IllegalArgumentException("PayPal validation failed: " + e.getMessage());
        }

        // reuse normal checkout flow
        return checkout(authentication, PaymentMethod.PAYPAL.name());
    }

    // ===== HELPER =====
    private void sendOrderConfirmationEmail(String email, OrderDTO orderDTO) {
        SettingBag emailSettings = settingService.getEmailSettings();

        // Create subject for email
        String subject = emailSettings.getValue("ORDER_CONFIRMATION_SUBJECT");
        subject = subject.replace("[[orderId]]", String.valueOf(orderDTO.getId()));

        // Create content for email
        String content = emailSettings.getValue("ORDER_CONFIRMATION_CONTENT");
        content = content.replace("[[name]]", orderDTO.getUserFullName());
        content = content.replace("[[orderId]]", String.valueOf(orderDTO.getId()));
        content = content.replace("[[shippingAddress]]", orderDTO.getAddress());
        content = content.replace("[[paymentMethod]]", orderDTO.getPaymentMethod().toString());

        SettingBag generalSettings = settingService.getGeneralSettingBag();
        float total = orderDTO.getTotal();
        if (orderDTO.getPaymentMethod() == PaymentMethod.PAYPAL) {
            total = (float) Math.round(total * 100) / 100;
        }
        String totalString = generalSettings.getValue("CURRENCY_SYMBOL") + " " + total;
        content = content.replace("[[total]]", totalString);

        // Set order time in content of email
        DateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss E, dd MMM yyyy");
        String orderTime = dateFormatter.format(orderDTO.getOrderTime());
        content = content.replace("[[orderTime]]", orderTime);

        // Send email
        MailUtil.sendEmail(emailSettings, email, subject, content);
    }
}
