package com.hip.damoa.domain.file.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.service.FileUploadService;
import com.hip.damoa.domain.file.web.dto.FileUploadCompleteRequest;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
import com.hip.damoa.domain.file.web.dto.PresignedUrlRequest;
import com.hip.damoa.domain.file.web.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 파일 업로드 REST API (Presigned URL 방식)
 */
@Slf4j
@Tag(name = "File", description = "파일 업로드 API (Presigned URL)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;

    /**
     * Presigned URL 생성 (1단계: 클라이언트가 S3 업로드용 URL 요청)
     */
    @Operation(summary = "Presigned URL 생성",
            description = "S3에 직접 업로드할 수 있는 Presigned URL을 생성합니다. " +
                    "클라이언트는 이 URL로 PUT 요청하여 파일을 업로드한 후 /api/files/complete를 호출해야 합니다.")
    @PostMapping("/presigned")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PresignedUrlRequest request) {

        PresignedUrlResponse response = fileUploadService.generatePresignedUrl(
                userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    /**
     * 파일 업로드 완료 처리 (2단계: 클라이언트가 S3 업로드 완료 후 호출)
     */
    @Operation(summary = "파일 업로드 완료",
            description = "S3 업로드 완료 후 호출하여 DB에 파일 정보를 저장합니다")
    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileUploadResponse> completeFileUpload(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FileUploadCompleteRequest request) {

        File file = fileUploadService.completeFileUpload(
                userDetails.getUsername(), request);

        return ApiResponse.success(FileUploadResponse.from(file));
    }

    /**
     * 엔티티별 파일 조회
     */
    @Operation(summary = "엔티티별 파일 조회",
            description = "특정 엔티티(업체, 포트폴리오 등)에 연결된 모든 파일을 조회합니다")
    @GetMapping("/entity")
    public ApiResponse<List<FileUploadResponse>> getFilesByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        List<File> files = fileUploadService.getFilesByEntity(entityType, entityId);

        List<FileUploadResponse> response = files.stream()
                .map(FileUploadResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 파일 삭제
     */
    @Operation(summary = "파일 삭제", description = "파일을 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{fileUuid}")
    public ApiResponse<Void> deleteFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID fileUuid) {

        fileUploadService.deleteFile(userDetails.getUsername(), fileUuid);
        return ApiResponse.success();
    }
}
