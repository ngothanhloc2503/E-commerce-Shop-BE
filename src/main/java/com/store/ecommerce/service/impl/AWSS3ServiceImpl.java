package com.store.ecommerce.service.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

    @Override
    public void uploadFile(String folderName, String fileName, InputStream inputStream) {
        var putObjectResult = s3Client.putObject(bucketName, folderName + "/" + fileName,
                inputStream, null);
        log.info(putObjectResult.getMetadata());
    }

    @Override
    public List<String> listFolder(String folderName) {
        ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request().withBucketName(bucketName)
                .withPrefix(folderName).withDelimiter("/");

        ListObjectsV2Result listing = s3Client.listObjectsV2(listObjectsRequest);

        List<String> listKeys = new ArrayList<>();

        for (S3ObjectSummary summary: listing.getObjectSummaries()) {
            listKeys.add(summary.getKey());
        }
        return listKeys;
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        } catch (AmazonServiceException e) {
            log.error("error [" + e.getMessage() + "] occurred while removing [" + fileName + "] file");
        }
    }

    @Override
    public void removeFolder(String folderName) {
        try {
            ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request().withBucketName(bucketName)
                    .withPrefix(folderName);

            ListObjectsV2Result listing = s3Client.listObjectsV2(listObjectsRequest);

            List<S3ObjectSummary> contents = listing.getObjectSummaries();

            for (S3ObjectSummary object : contents) {
                s3Client.deleteObject(new DeleteObjectRequest(bucketName, object.getKey()));
            }
        } catch (AmazonServiceException e) {
            log.error("error [" + e.getMessage() + "] occurred while removing [" + folderName + "] folder");
        }
    }

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
