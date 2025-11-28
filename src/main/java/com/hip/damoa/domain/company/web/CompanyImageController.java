package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.service.CompanyImageService;
import com.hip.damoa.domain.company.web.dto.CompanyImageRequest;
import com.hip.damoa.domain.company.web.dto.CompanyImageResponse;
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
 * 업체 이미지 관리 REST API
 */
@Slf4j
@Tag(name = "06. Company Image", description = "업체 이미지 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/{companyUuid}/images")
public class CompanyImageController {

    private final CompanyImageService companyImageService;

    /**
     * 업체 이미지 추가
     */
    @Operation(summary = "업체 이미지 추가",
            description = "업체에 이미지를 추가합니다.\n\n" +
                    "**이미지 업로드 프로세스:**\n" +
                    "1. `/api/files/presigned` 호출 → Presigned URL 획득\n" +
                    "2. S3로 파일 직접 업로드 (PUT 요청)\n" +
                    "3. `/api/files/complete` 호출 → 파일 ID 획득\n" +
                    "4. **이 API 호출** → 획득한 파일 ID를 fileId로 전송\n\n" +
                    "**이미지 타입:**\n" +
                    "- `LOGO`: 업체 로고\n" +
                    "- `COVER`: 커버 이미지\n" +
                    "- `GALLERY`: 갤러리 이미지\n" +
                    "- `PORTFOLIO`: 포트폴리오/시공사례\n" +
                    "- `CERTIFICATE`: 자격증/인증서\n\n" +
                    "**필수 정보:**\n" +
                    "- fileId: 파일 업로드 완료 후 받은 파일 ID\n" +
                    "- imageType: 이미지 타입\n\n" +
                    "**선택 정보:**\n" +
                    "- displayOrder: 표시 순서 (기본: 0)\n" +
                    "- isPrimary: 대표 이미지 여부 (기본: false)\n" +
                    "- caption: 이미지 설명\n\n" +
                    "**권한:**\n" +
                    "- 업체 소유자만 추가 가능")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyImageResponse> addCompanyImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @Valid @RequestBody CompanyImageRequest request) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        log.info("업체 이미지 추가: userEmail={}, companyUuid={}, imageType={}",
                userDetails.getUsername(), companyUuid, request.getImageType());
        CompanyImage image = companyImageService.addCompanyImage(
                userDetails.getUsername(), companyUuid, request);
        return ApiResponse.success(companyImageService.toResponse(image));
    }

    /**
     * 업체 이미지 목록 조회
     */
    @Operation(summary = "업체 이미지 목록",
            description = "업체의 모든 이미지를 조회합니다.\n\n" +
                    "**응답:**\n" +
                    "- 모든 타입의 이미지 목록\n" +
                    "- displayOrder 순으로 정렬\n" +
                    "- 각 이미지의 URL, 타입, 대표 이미지 여부 포함\n\n" +
                    "**활용:**\n" +
                    "- 업체 상세 페이지 이미지 갤러리\n" +
                    "- 업체 프로필 이미지 표시\n" +
                    "- 포트폴리오 목록")
    @GetMapping
    public ApiResponse<List<CompanyImageResponse>> getCompanyImages(@PathVariable UUID companyUuid) {
        List<CompanyImage> images = companyImageService.getCompanyImages(companyUuid);

        List<CompanyImageResponse> response = images.stream()
                .map(companyImageService::toResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 업체 이미지 타입별 조회
     */
    @Operation(summary = "업체 이미지 타입별 조회",
            description = "특정 타입의 이미지만 조회합니다.\n\n" +
                    "**이미지 타입:**\n" +
                    "- `LOGO`: 로고 이미지만\n" +
                    "- `COVER`: 커버 이미지만\n" +
                    "- `GALLERY`: 갤러리 이미지만\n" +
                    "- `PORTFOLIO`: 포트폴리오만\n" +
                    "- `CERTIFICATE`: 자격증/인증서만\n\n" +
                    "**활용 예시:**\n" +
                    "- `/api/companies/{uuid}/images/type/LOGO` → 로고만 조회\n" +
                    "- `/api/companies/{uuid}/images/type/PORTFOLIO` → 포트폴리오만 조회\n\n" +
                    "**응답:**\n" +
                    "- 해당 타입의 이미지 목록\n" +
                    "- displayOrder 순으로 정렬")
    @GetMapping("/type/{imageType}")
    public ApiResponse<List<CompanyImageResponse>> getCompanyImagesByType(
            @PathVariable UUID companyUuid,
            @PathVariable String imageType) {

        List<CompanyImage> images = companyImageService.getCompanyImagesByType(companyUuid, imageType);

        List<CompanyImageResponse> response = images.stream()
                .map(companyImageService::toResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 대표 이미지 설정
     */
    @Operation(summary = "대표 이미지 설정",
            description = "이미지를 대표 이미지로 설정합니다.\n\n" +
                    "**동작:**\n" +
                    "- 선택한 이미지를 isPrimary=true로 설정\n" +
                    "- 동일한 업체의 다른 모든 이미지는 isPrimary=false로 변경\n" +
                    "- 한 업체당 하나의 대표 이미지만 존재\n\n" +
                    "**권한:**\n" +
                    "- 업체 소유자만 설정 가능\n\n" +
                    "**활용:**\n" +
                    "- 업체 목록에서 표시할 대표 이미지 선택\n" +
                    "- 검색 결과에서 보여질 이미지 설정")
    @PatchMapping("/{imageUuid}/primary")
    public ApiResponse<CompanyImageResponse> setPrimaryImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID imageUuid) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        log.info("대표 이미지 설정: userEmail={}, companyUuid={}, imageUuid={}",
                userDetails.getUsername(), companyUuid, imageUuid);
        CompanyImage image = companyImageService.setPrimaryImage(userDetails.getUsername(), imageUuid);
        return ApiResponse.success(companyImageService.toResponse(image));
    }

    /**
     * 업체 이미지 삭제
     */
    @Operation(summary = "업체 이미지 삭제",
            description = "업체 이미지를 삭제합니다.\n\n" +
                    "**삭제 방식:**\n" +
                    "- Soft Delete (isDeleted=true)\n" +
                    "- S3 파일은 즉시 삭제되지 않음 (추후 배치 작업으로 정리)\n\n" +
                    "**권한:**\n" +
                    "- 업체 소유자만 삭제 가능\n\n" +
                    "**주의사항:**\n" +
                    "- 대표 이미지 삭제 시 다른 이미지를 대표로 재설정 필요\n" +
                    "- 삭제 후 복구 불가")
    @DeleteMapping("/{imageUuid}")
    public ApiResponse<Void> deleteCompanyImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID imageUuid) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        log.info("업체 이미지 삭제: userEmail={}, companyUuid={}, imageUuid={}",
                userDetails.getUsername(), companyUuid, imageUuid);
        companyImageService.deleteCompanyImage(userDetails.getUsername(), imageUuid);
        return ApiResponse.success();
    }
}
