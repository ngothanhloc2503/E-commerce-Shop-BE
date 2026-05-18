package com.store.ecommerce.config;

import com.paypal.sdk.ClientCredentialsAuth;
import com.paypal.sdk.controllers.OrdersController;
import com.store.ecommerce.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PaypalConfig {
    
    private final SettingService settingService;
    
    @Bean
    public com.paypal.sdk.Configuration paypalConfiguration() {
        var paymentSettings = settingService.getPaymentSettings();
        String clientId = paymentSettings.getValue("PAYPAL_API_CLIENT_ID");
        String clientSecret = paymentSettings.getValue("PAYPAL_API_CLIENT_SECRET");
        String baseUrl = paymentSettings.getValue("PAYPAL_API_BASE_URL");
        
        log.info("Initializing PayPal SDK with base URL: {}", baseUrl);
        
        return new com.paypal.sdk.Configuration.Builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .baseUrl(baseUrl)
            .build();
    }
    
    @Bean
    public OrdersController ordersController(com.paypal.sdk.Configuration paypalConfiguration) {
        return new OrdersController(paypalConfiguration);
    }
}
