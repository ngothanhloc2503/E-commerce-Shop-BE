package com.store.ecommerce.service;

import java.io.InputStream;
import java.util.List;

public interface AWSS3Service {
    void uploadFile(String folderName, String fileName, InputStream inputStream);

    List<String> listFolder(String folderName);

    void deleteFile(String fileName);

    void removeFolder(String folderName);

    String getBaseURI();

    String getImagePath(String folderName, String fileName);
}
