package com.store.ecommerce.util;

import jakarta.servlet.http.HttpServletResponse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExporterUtil {
    public static void setResponseHeader(HttpServletResponse response, String contentType, String extension, String prefix) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormatter.format(new Date());
        String fileName = prefix + timestamp + extension;

        response.setContentType(contentType);

        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", fileName);
        response.setHeader(headerKey, headerValue);
    }
}
