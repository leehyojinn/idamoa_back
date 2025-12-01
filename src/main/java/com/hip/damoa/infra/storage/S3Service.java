package com.hip.damoa.infra.storage;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * AWS S3 Service
 *
 * Handles file upload, download, and deletion operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    @Value("${aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    /**
     * Upload file to S3
     */
    public UploadResult uploadFile(MultipartFile file, String directory) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = generateUniqueFilename(extension);
            String s3Key = directory + "/" + storedFilename;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .acl(ObjectCannedACL.PUBLIC_READ)  // 퍼블릭 읽기 권한 설정
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String publicUrl = getPublicUrl(s3Key);

            log.info("File uploaded to S3: bucket={}, key={}, size={}", bucket, s3Key, file.getSize());

            return UploadResult.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .s3Key(s3Key)
                .s3Url(publicUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .fileExtension(extension)
                .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("S3 error while uploading file", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("File deleted from S3: bucket={}, key={}", bucket, s3Key);

        } catch (S3Exception e) {
            log.error("Failed to delete file from S3: key={}", s3Key, e);
            // Don't throw exception - file might not exist
        }
    }

    /**
     * Generate pre-signed URL for temporary access
     */
    public String generatePresignedUrl(String s3Key, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();

        } catch (S3Exception e) {
            log.error("Failed to generate presigned URL: key={}", s3Key, e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence: key={}", s3Key, e);
            return false;
        }
    }

    /**
     * Get public URL (CloudFront or S3)
     */
    private String getPublicUrl(String s3Key) {
        if (cloudFrontDomain != null && !cloudFrontDomain.isEmpty()) {
            return cloudFrontDomain + "/" + s3Key;
        }
        return baseUrl + "/" + s3Key;
    }

    /**
     * Generate unique filename
     */
    private String generateUniqueFilename(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
