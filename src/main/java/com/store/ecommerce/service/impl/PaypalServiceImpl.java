package com.store.ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.service.PaypalService;
import com.store.ecommerce.service.SettingService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpStatus.OK;

@Component
public class PaypalServiceImpl implements PaypalService {

    @Autowired
    private SettingService settingService;

    @Override
    public boolean validateOrder(String orderId) throws BadRequestException {
        SettingBag paymentSettings = settingService.getPaymentSettings();
        String baseUrl = paymentSettings.getValue("PAYPAL_API_BASE_URL");
        String clientId = paymentSettings.getValue("PAYPAL_API_CLIENT_ID");
        String clientSecret = paymentSettings.getValue("PAYPAL_API_CLIENT_SECRET");

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(clientId, clientSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Accept-Language", "en_US");

            String requestURL = baseUrl + "/v2/checkout/orders/" + orderId;

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(requestURL, HttpMethod.GET, entity, String.class);

            // Parse the JSON response to extract the orderId
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String responseOrderId = responseBody.get("id").asText(); // Assuming the order ID is under the 'id' key

            HttpStatus statusCode = (HttpStatus) response.getStatusCode();
            String errorMessage = null;
            if (statusCode.equals(OK)) {
                return responseOrderId.equals(orderId);
            } else {
                switch (statusCode) {
                    case NOT_FOUND:
                        errorMessage = "Order ID not found";
                    case BAD_REQUEST:
                        errorMessage = "Bad Request to PayPal Checkout API";
                    case INTERNAL_SERVER_ERROR:
                        errorMessage = "PayPal server error";
                    default:
                        errorMessage = "PayPal returned none-OK status code";
                }

                throw new BadRequestException(errorMessage);
            }
        } catch (BadRequestException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            throw new BadRequestException("Payment validation failed.");
        }
    }
}
