package com.store.ecommerce.controller;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.service.CheckoutService;
import com.store.ecommerce.service.OrderService;
import com.store.ecommerce.service.PaypalService;
import com.store.ecommerce.service.SettingService;
import com.store.ecommerce.util.MailUtil;
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
public class CheckoutController {
    private final OrderService orderService;
    private final CheckoutService checkoutService;
    private final SettingService settingService;
    private final PaypalService paypalService;

    @GetMapping("")
    public ResponseEntity<?> getCheckoutInformation(Authentication authentication) {

        return ResponseEntity.ok(checkoutService.getCheckoutInformation(authentication.getName()));
    }

    @PostMapping("")
    public ResponseEntity<?> checkout(Authentication authentication,
                                      @RequestParam("paymentMethod") String paymentType) {
        PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentType);

        String email = authentication.getName();
        OrderDTO order = orderService.createOrder(email, paymentMethod);
        sendOrderConfirmationEmail(email, order);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/paypal")
    public ResponseEntity<?> processPayPalCheckout(Authentication authentication,
                                                   @RequestBody Map<String, String> request) throws UnsupportedEncodingException {
        try {
            String orderId = request.get("orderId");
            if (orderId == null || orderId.isEmpty()) {
                return new ResponseEntity<>("orderId is required", HttpStatus.BAD_REQUEST);
            }

            if (paypalService.validateOrder(orderId)) {
                return checkout(authentication, String.valueOf(PaymentMethod.PAYPAL));
            } else {
                return new ResponseEntity<>("Transaction could not be completed because order information is invalid!", HttpStatus.BAD_REQUEST);
            }
        } catch (BadRequestException e) {
            return new ResponseEntity<>("Transaction failed due to error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

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
