package com.hip.damoa.domain.company.web;

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
    @Operation(summary = "업체 이미지 추가", description = "업체에 이미지를 추가합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyImageResponse> addCompanyImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @Valid @RequestBody CompanyImageRequest request) {

        CompanyImage image = companyImageService.addCompanyImage(
                userDetails.getUsername(), companyUuid, request);

        return ApiResponse.success(CompanyImageResponse.from(image));
    }

    /**
     * 업체 이미지 목록 조회
     */
    @Operation(summary = "업체 이미지 목록", description = "업체의 모든 이미지를 조회합니다")
    @GetMapping
    public ApiResponse<List<CompanyImageResponse>> getCompanyImages(@PathVariable UUID companyUuid) {
        List<CompanyImage> images = companyImageService.getCompanyImages(companyUuid);

        List<CompanyImageResponse> response = images.stream()
                .map(CompanyImageResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 업체 이미지 타입별 조회
     */
    @Operation(summary = "업체 이미지 타입별 조회", description = "특정 타입의 이미지만 조회합니다")
    @GetMapping("/type/{imageType}")
    public ApiResponse<List<CompanyImageResponse>> getCompanyImagesByType(
            @PathVariable UUID companyUuid,
            @PathVariable String imageType) {

        List<CompanyImage> images = companyImageService.getCompanyImagesByType(companyUuid, imageType);

        List<CompanyImageResponse> response = images.stream()
                .map(CompanyImageResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 대표 이미지 설정
     */
    @Operation(summary = "대표 이미지 설정", description = "이미지를 대표 이미지로 설정합니다")
    @PatchMapping("/{imageUuid}/primary")
    public ApiResponse<CompanyImageResponse> setPrimaryImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID imageUuid) {

        CompanyImage image = companyImageService.setPrimaryImage(userDetails.getUsername(), imageUuid);
        return ApiResponse.success(CompanyImageResponse.from(image));
    }

    /**
     * 업체 이미지 삭제
     */
    @Operation(summary = "업체 이미지 삭제", description = "업체 이미지를 삭제합니다")
    @DeleteMapping("/{imageUuid}")
    public ApiResponse<Void> deleteCompanyImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID imageUuid) {

        companyImageService.deleteCompanyImage(userDetails.getUsername(), imageUuid);
        return ApiResponse.success();
    }
}
