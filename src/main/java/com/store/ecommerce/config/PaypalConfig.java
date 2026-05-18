package com.store.ecommerce.config;

import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.service.SettingService;
import jakarta.annotation.PreDestroy;
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
    public PaypalServerSdkClient paypalClient() {
        SettingBag paymentSettings = settingService.getPaymentSettings();
        String clientId = paymentSettings.getValue("PAYPAL_API_CLIENT_ID");
        String clientSecret = paymentSettings.getValue("PAYPAL_API_CLIENT_SECRET");
        String baseUrl = paymentSettings.getValue("PAYPAL_API_BASE_URL");

        Environment environment = baseUrl.contains("sandbox")
                ? Environment.SANDBOX
                : Environment.PRODUCTION;

        log.info("Initializing PayPal client [environment={}]", environment);

        return new PaypalServerSdkClient.Builder()
                .clientCredentialsAuth(
                        new ClientCredentialsAuthModel.Builder(clientId, clientSecret)
                                .build()
                )
                .environment(environment)
                .httpClientConfig(configBuilder -> configBuilder.timeout(30000))
                .build();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down PayPal client");
    }
}