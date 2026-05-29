package com.store.ecommerce.common;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constants {
    public static final int DIM_DIVISOR = 139;

    @Getter
    @Value("${app.frontend.url:https://e-commerce-shop-fe.onrender.com}")
    private String feUrl;

    @Getter
    private static String staticFeUrl = "https://e-commerce-shop-fe.onrender.com";

    @Value("${app.frontend.url:https://e-commerce-shop-fe.onrender.com}")
    public void setStaticFeUrl(String feUrl) {
        staticFeUrl = feUrl;
    }
}
