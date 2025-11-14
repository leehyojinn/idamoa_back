package com.hip.damoa.domain.file.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.service.FileUploadService;
import com.hip.damoa.domain.file.web.dto.FileUploadCompleteRequest;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
import com.hip.damoa.domain.file.web.dto.PresignedUrlRequest;
import com.hip.damoa.domain.file.web.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "04. File", description = "파일 업로드 API (Presigned URL)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;

    /**
     * Presigned URL 생성 (1단계: 클라이언트가 S3 업로드용 URL 요청)
     */
    @Operation(summary = "Presigned URL 생성 (1/2단계)",
            description = "S3에 직접 업로드할 수 있는 Presigned URL을 생성합니다.\n\n" +
                    "**파일 업로드 전체 플로우:**\n" +
                    "1. **이 API 호출** - Presigned URL 요청\n" +
                    "2. **클라이언트가 S3로 직접 업로드** - 받은 presignedUrl로 PUT 요청\n" +
                    "3. **업로드 완료 API 호출** - `/api/files/complete` 호출하여 DB 저장\n\n" +
                    "**요청 필수 정보:**\n" +
                    "- filename: 원본 파일명 (예: `profile.jpg`)\n" +
                    "- mimeType: MIME 타입 (예: `image/jpeg`, `application/pdf`)\n" +
                    "- fileSize: 파일 크기 (bytes)\n\n" +
                    "**선택 정보 (엔티티 연결):**\n" +
                    "- entityType: `COMPANY_IMAGE`, `PORTFOLIO`, `ESTIMATE_ATTACHMENT` 등\n" +
                    "- entityId: 연결할 대상의 ID\n\n" +
                    "**응답 정보:**\n" +
                    "- presignedUrl: 이 URL로 파일을 PUT 요청\n" +
                    "- uploadId: 완료 처리 시 필요 (저장 필수)\n" +
                    "- fileKey: S3 파일 키 (저장 필수)\n" +
                    "- expiresIn: URL 유효 시간 (기본 3600초 = 1시간)\n\n" +
                    "**프론트엔드 예시 (JavaScript):**\n" +
                    "```javascript\n" +
                    "// 1. Presigned URL 요청\n" +
                    "const response = await fetch('/api/files/presigned', {\n" +
                    "  method: 'POST',\n" +
                    "  headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + accessToken },\n" +
                    "  body: JSON.stringify({\n" +
                    "    filename: file.name,\n" +
                    "    mimeType: file.type,\n" +
                    "    fileSize: file.size,\n" +
                    "    entityType: 'COMPANY_IMAGE',\n" +
                    "    entityId: 123\n" +
                    "  })\n" +
                    "});\n" +
                    "const data = await response.json();\n\n" +
                    "// 2. S3로 직접 업로드 (백엔드 거치지 않음 - 빠름)\n" +
                    "await fetch(data.data.presignedUrl, {\n" +
                    "  method: 'PUT',\n" +
                    "  body: file,\n" +
                    "  headers: { 'Content-Type': file.type }\n" +
                    "});\n\n" +
                    "// 3. 업로드 완료 알림\n" +
                    "await fetch('/api/files/complete', {\n" +
                    "  method: 'POST',\n" +
                    "  headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + accessToken },\n" +
                    "  body: JSON.stringify({\n" +
                    "    uploadId: data.data.uploadId,\n" +
                    "    fileKey: data.data.fileKey\n" +
                    "  })\n" +
                    "});\n" +
                    "```\n\n" +
                    "**장점:**\n" +
                    "- 백엔드 부하 감소 (파일이 백엔드를 거치지 않음)\n" +
                    "- 업로드 속도 향상 (S3 직접 연결)\n" +
                    "- 대용량 파일 업로드 가능")
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
    @Operation(summary = "파일 업로드 완료 (2/2단계)",
            description = "S3 업로드 완료 후 호출하여 DB에 파일 정보를 저장합니다.\n\n" +
                    "**호출 시점:**\n" +
                    "- S3로 파일 업로드 완료 후 즉시 호출\n" +
                    "- Presigned URL로 PUT 요청이 성공(200 OK)한 직후\n\n" +
                    "**필수 정보:**\n" +
                    "- uploadId: `/api/files/presigned` 응답에서 받은 값\n" +
                    "- fileKey: `/api/files/presigned` 응답에서 받은 값\n\n" +
                    "**선택 정보 (이미지인 경우):**\n" +
                    "- width: 이미지 가로 크기 (픽셀)\n" +
                    "- height: 이미지 세로 크기 (픽셀)\n" +
                    "- description: 파일 설명\n\n" +
                    "**처리 내용:**\n" +
                    "1. S3에 파일이 실제로 존재하는지 확인\n" +
                    "2. 파일 정보를 DB에 저장\n" +
                    "3. 파일 접근 URL 생성 (CDN 또는 S3 URL)\n" +
                    "4. 엔티티와 연결 (entityType/entityId가 있는 경우)\n\n" +
                    "**응답:**\n" +
                    "- fileUrl: 파일에 접근할 수 있는 공개 URL\n" +
                    "- uuid: 파일 고유 식별자\n" +
                    "- 파일 메타데이터 (크기, MIME 타입 등)\n\n" +
                    "**주의사항:**\n" +
                    "- S3 업로드 실패 시 이 API를 호출하지 마세요\n" +
                    "- 동일한 uploadId로 중복 호출하면 409 Conflict 발생\n" +
                    "- Presigned URL 만료 전에 업로드와 완료 처리를 모두 완료해야 합니다")
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
            description = "특정 엔티티(업체, 포트폴리오 등)에 연결된 모든 파일을 조회합니다.\n\n" +
                    "**사용 예시:**\n" +
                    "- 업체 프로필 이미지 조회: `entityType=COMPANY_IMAGE&entityId=123`\n" +
                    "- 포트폴리오 이미지 조회: `entityType=PORTFOLIO&entityId=456`\n" +
                    "- 견적 첨부파일 조회: `entityType=ESTIMATE_ATTACHMENT&entityId=789`\n" +
                    "- 게시판 첨부파일 조회: `entityType=BOARD_ATTACHMENT&entityId=101`\n\n" +
                    "**지원하는 엔티티 타입:**\n" +
                    "- `COMPANY_IMAGE`: 업체 이미지 (로고, 사진 등)\n" +
                    "- `PORTFOLIO`: 포트폴리오/시공사례 이미지\n" +
                    "- `ESTIMATE_ATTACHMENT`: 견적 요청 첨부파일\n" +
                    "- `BOARD_ATTACHMENT`: 게시판 첨부파일\n" +
                    "- `REVIEW_IMAGE`: 리뷰 이미지\n\n" +
                    "**응답:**\n" +
                    "- 파일 목록 배열 (생성일 기준 정렬)\n" +
                    "- 각 파일의 URL, 크기, MIME 타입 등 메타데이터\n" +
                    "- 삭제되지 않은(isDeleted=false) 파일만 반환\n\n" +
                    "**활용:**\n" +
                    "- 업체 상세 페이지에서 이미지 갤러리 표시\n" +
                    "- 견적 요청서 첨부파일 목록 표시\n" +
                    "- 게시글 첨부파일 다운로드 링크 제공")
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
    @Operation(summary = "파일 삭제",
            description = "파일을 삭제합니다 (Soft Delete).\n\n" +
                    "**삭제 방식:**\n" +
                    "- **Soft Delete**: DB에서 `isDeleted=true`로 표시 (실제 파일은 남음)\n" +
                    "- S3에서 파일은 즉시 삭제되지 않음 (추후 배치 작업으로 정리)\n" +
                    "- 삭제된 파일은 목록 조회 시 제외됨\n\n" +
                    "**권한:**\n" +
                    "- 파일 업로드자 본인만 삭제 가능\n" +
                    "- 다른 사용자가 삭제 시도 시 403 Forbidden\n\n" +
                    "**파일 식별:**\n" +
                    "- fileUuid: 파일 고유 UUID 사용 (Long ID가 아님)\n" +
                    "- 예: `550e8400-e29b-41d4-a716-446655440000`\n\n" +
                    "**응답:**\n" +
                    "- 성공 시 200 OK (body 없음)\n" +
                    "- 파일 없음 시 404 Not Found\n" +
                    "- 권한 없음 시 403 Forbidden\n\n" +
                    "**주의사항:**\n" +
                    "- 삭제 후 복구 불가 (Soft Delete이지만 UI에서 접근 불가)\n" +
                    "- 엔티티와 연결된 파일도 삭제됨 (연결 정보도 함께 삭제)\n" +
                    "- 삭제 전 사용자 확인 권장 (프론트에서 확인 다이얼로그 표시)")
    @DeleteMapping("/{fileUuid}")
    public ApiResponse<Void> deleteFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID fileUuid) {

        fileUploadService.deleteFile(userDetails.getUsername(), fileUuid);
        return ApiResponse.success();
    }
}
