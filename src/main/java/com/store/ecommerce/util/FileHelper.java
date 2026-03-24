package com.store.ecommerce.util;

import org.springframework.web.multipart.MultipartFile;

public class FileHelper {

    public static boolean isFileNullOrEmpty(MultipartFile file) {
        if (file == null) {
            return true;
        }
        return file.isEmpty();
    }
}
