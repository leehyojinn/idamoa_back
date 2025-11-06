package com.hip.damoa.domain.file.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.file.web.dto.FileUploadCompleteRequest;
import com.hip.damoa.domain.file.web.dto.PresignedUrlRequest;
import com.hip.damoa.domain.file.web.dto.PresignedUrlResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.infra.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 파일 업로드 서비스 (Presigned URL 방식)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration UPLOAD_METADATA_TTL = Duration.ofMinutes(30);
    private static final String UPLOAD_METADATA_PREFIX = "file:upload:";

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    /**
     * Presigned URL 생성 (클라이언트가 직접 S3에 업로드)
     */
    @Transactional(readOnly = true)
    public PresignedUrlResponse generatePresignedUrl(String userEmail, PresignedUrlRequest request) {
        log.info("Presigned URL 생성: userEmail={}, filename={}", userEmail, request.getFilename());

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 파일 검증
        validateFileMetadata(request);

        // S3 키 생성
        String uploadId = UUID.randomUUID().toString();
        String fileKey = generateFileKey(request.getFilename());

        // Presigned URL 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(request.getMimeType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();

        // Redis에 업로드 메타데이터 저장 (나중에 완료 처리 시 사용)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", user.getId());
        metadata.put("userEmail", user.getEmail());
        metadata.put("filename", request.getFilename());
        metadata.put("mimeType", request.getMimeType());
        metadata.put("fileSize", request.getFileSize());
        metadata.put("fileKey", fileKey);
        metadata.put("entityType", request.getEntityType());
        metadata.put("entityId", request.getEntityId());

        String redisKey = UPLOAD_METADATA_PREFIX + uploadId;
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            redisService.setValues(redisKey, metadataJson, UPLOAD_METADATA_TTL);
        } catch (JsonProcessingException e) {
            log.error("메타데이터 JSON 변환 실패", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        log.info("Presigned URL 생성 완료: uploadId={}, fileKey={}", uploadId, fileKey);

        return PresignedUrlResponse.builder()
                .uploadId(uploadId)
                .presignedUrl(presignedUrl)
                .fileKey(fileKey)
                .filename(request.getFilename())
                .expiresIn(PRESIGNED_URL_EXPIRATION.getSeconds())
                .callbackUrl("/api/files/complete")
                .build();
    }

    /**
     * 파일 업로드 완료 처리 (클라이언트가 S3 업로드 완료 후 호출)
     */
    @Transactional
    public File completeFileUpload(String userEmail, FileUploadCompleteRequest request) {
        log.info("파일 업로드 완료 처리: uploadId={}", request.getUploadId());

        // Redis에서 업로드 메타데이터 조회
        String redisKey = UPLOAD_METADATA_PREFIX + request.getUploadId();
        String metadataStr = redisService.getValues(redisKey);

        if (metadataStr == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        // 메타데이터 파싱 (간단히 처리, 실제로는 JSON 파싱 필요)
        Map<String, Object> metadata = parseMetadata(metadataStr);

        // 사용자 확인
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        log.info("metadata: {}",metadata);
        if (!user.getEmail().equals(metadata.get("userEmail"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // File 엔티티 생성
        String fileUrl = generateFileUrl(request.getFileKey());
        String fileExtension = getFileExtension((String) metadata.get("filename"));

        Map<String, Object> imageMetadata = new HashMap<>();
        if (request.getWidth() != null) {
            imageMetadata.put("width", request.getWidth());
        }
        if (request.getHeight() != null) {
            imageMetadata.put("height", request.getHeight());
        }

        File file = File.builder()
                .originalFilename((String) metadata.get("filename"))
                .storedFilename(extractFilenameFromKey(request.getFileKey()))
                .filePath(request.getFileKey())
                .fileUrl(fileUrl)
                .fileSize(toLong(metadata.get("fileSize")))
                .mimeType((String) metadata.get("mimeType"))
                .fileExtension(fileExtension)
                .uploader(user)
                .entityType((String) metadata.get("entityType"))
                .entityId(toLong(metadata.get("entityId")))
                .imageMetadata(imageMetadata)
                .description(request.getDescription())
                .isPublic(true)
                .build();

        file = fileRepository.save(file);

        // Redis에서 메타데이터 삭제
        redisService.deleteValues(redisKey);

        log.info("파일 업로드 완료: id={}, url={}", file.getId(), fileUrl);

        return file;
    }

    /**
     * 파일 삭제
     */
    @Transactional
    public void deleteFile(String userEmail, Long fileId) {
        log.info("파일 삭제: fileId={}, userEmail={}", fileId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // 권한 확인 (업로더 또는 관리자)
        if (!file.getUploader().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // S3에서 삭제
        deleteFromS3(file.getFilePath());

        // DB에서 삭제 (Soft Delete)
        file.softDelete();
        fileRepository.save(file);

        log.info("파일 삭제 완료: id={}", fileId);
    }

    /**
     * 엔티티의 모든 파일 조회
     */
    @Transactional(readOnly = true)
    public List<File> getFilesByEntity(String entityType, Long entityId) {
        return fileRepository.findByEntityTypeAndEntityIdOrderByDisplayOrder(entityType, entityId);
    }

    /**
     * 파일 메타데이터 검증
     */
    private void validateFileMetadata(PresignedUrlRequest request) {
        if (request.getFileSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String mimeType = request.getMimeType();
        if (mimeType != null && !ALLOWED_IMAGE_TYPES.contains(mimeType.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    /**
     * S3 파일 키 생성
     */
    private String generateFileKey(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);
        String storedFilename = String.format("%s_%s.%s", timestamp, uuid, extension);

        return "uploads/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + storedFilename;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 파일 키에서 파일명 추출
     */
    private String extractFilenameFromKey(String fileKey) {
        if (fileKey == null) {
            return "";
        }
        return fileKey.substring(fileKey.lastIndexOf("/") + 1);
    }

    /**
     * S3에서 파일 삭제
     */
    private void deleteFromS3(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("S3 삭제 완료: key={}", s3Key);
        } catch (Exception e) {
            log.error("S3 삭제 실패: key={}", s3Key, e);
            // S3 삭제 실패는 로그만 남기고 계속 진행
        }
    }

    /**
     * 파일 URL 생성
     */
    private String generateFileUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }

    /**
     * 메타데이터 파싱
     */
    private Map<String, Object> parseMetadata(String metadataStr) {
        try {
            return objectMapper.readValue(metadataStr, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("메타데이터 JSON 파싱 실패: {}", metadataStr, e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * Object를 Long으로 안전하게 변환
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Long");
    }
}
