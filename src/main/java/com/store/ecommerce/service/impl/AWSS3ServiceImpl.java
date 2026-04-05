package com.store.ecommerce.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.store.ecommerce.service.AWSS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class AWSS3ServiceImpl implements AWSS3Service {
    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    // ====== UPLOAD ======
    @Override
    public void uploadFile(String folderName, String fileName,
                           InputStream inputStream,
                           long contentLength,
                           String contentType) {

        try {
            String key = buildKey(folderName, fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType);

            s3Client.putObject(bucketName, key, inputStream, metadata);

            log.info("Uploaded file: {}", key);

        } catch (Exception e) {
            log.error("Upload failed: {}/{}", folderName, fileName, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    // ====== LIST ======
    @Override
    public List<String> listFolder(String folderName) {
        List<String> keys = new ArrayList<>();

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(normalizeFolder(folderName));

        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary summary : result.getObjectSummaries()) {
                keys.add(summary.getKey());
            }

            request.setContinuationToken(result.getNextContinuationToken());

        } while (result.isTruncated());

        return keys;
    }

    // ====== DELETE FILE ======
    @Override
    public void deleteFile(String fileName) {
        try {
            s3Client.deleteObject(bucketName, fileName);
            log.info("Deleted file: {}", fileName);
        } catch (Exception e) {
            log.error("Delete file failed: {}", fileName, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    // ====== DELETE FOLDER ======
    @Override
    public void removeFolder(String folderName) {
        List<String> keys = listFolder(folderName);

        if (keys.isEmpty()) return;

        try {
            List<DeleteObjectsRequest.KeyVersion> keyVersions = keys.stream()
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .toList();

            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName)
                    .withKeys(keyVersions);

            s3Client.deleteObjects(request);

            log.info("Deleted folder: {}", folderName);

        } catch (Exception e) {
            log.error("Delete folder failed: {}", folderName, e);
            throw new RuntimeException("Failed to delete folder", e);
        }
    }

    // ====== MOVE FOLDER ======
    @Override
    public void moveFolder(String sourceFolder, String destinationFolder) {
        List<String> keys = listFolder(sourceFolder);

        if (keys.isEmpty()) return;

        try {
            for (String oldKey : keys) {

                String newKey = oldKey.replaceFirst(
                        normalizeFolder(sourceFolder),
                        normalizeFolder(destinationFolder)
                );

                // Copy
                s3Client.copyObject(bucketName, oldKey, bucketName, newKey);

                log.info("Copied: {} -> {}", oldKey, newKey);
            }

            // Delete old after copy success
            removeFolder(sourceFolder);

        } catch (Exception e) {
            log.error("Move folder failed: {} -> {}", sourceFolder, destinationFolder, e);
            throw new RuntimeException("Failed to move folder", e);
        }
    }

    // ====== HELPER ======
    private String buildKey(String folder, String file) {
        return normalizeFolder(folder) + file;
    }

    private String normalizeFolder(String folder) {
        return folder.endsWith("/") ? folder : folder + "/";
    }

    // ====== URL ======
    @Override
    public String getBaseURI() {
        String pattern = "https://%s.s3.%s.amazonaws.com";
        return String.format(pattern, bucketName, region);
    }

    @Override
    public String getImagePath(String folderName, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            if (folderName.startsWith("user")) {
                return getBaseURI() + "/images/default-user.png";
            } else {
                return getBaseURI() + "/images/image_thumbnail.png";
            }
        }

        return getBaseURI() + "/" + folderName + "/" + fileName;
    }
}
