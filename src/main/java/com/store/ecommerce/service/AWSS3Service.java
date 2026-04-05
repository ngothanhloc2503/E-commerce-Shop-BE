package com.store.ecommerce.service;

import java.io.InputStream;
import java.util.List;

public interface AWSS3Service {
    void uploadFile(String folderName, String fileName,
                    InputStream inputStream,
                    long contentLength,
                    String contentType);

    List<String> listFolder(String folderName);

    void deleteFile(String fileName);

    void removeFolder(String folderName);

    void moveFolder(String sourceFolder, String destinationFolder);

    String getBaseURI();

    String getImagePath(String folderName, String fileName);
}
