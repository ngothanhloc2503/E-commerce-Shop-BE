package com.store.ecommerce.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class CustomSensitiveSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = auth != null && auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_CUSTOMER"));

        if (value instanceof String) {
            gen.writeString(isCustomer ? "********" : (String) value);
        } else if (value instanceof Integer) {
            gen.writeNumber(isCustomer ? 0 : (Integer) value);
        } else if (value instanceof Float) {
            gen.writeNumber(isCustomer ? 0.0f : (Float) value);
        } else if (value instanceof Double) {
            gen.writeNumber(isCustomer ? 0.0 : (Double) value);
        } else if (value instanceof Boolean) {
            gen.writeBoolean(isCustomer && (Boolean) value);
        } else {
            gen.writeObject(value);
        }
    }
}
