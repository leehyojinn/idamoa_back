package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.file.service.AdminFilePricingService;
import com.hip.damoa.domain.file.web.dto.FilePricingRequest;
import com.hip.damoa.domain.file.web.dto.FilePricingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin - File", description = "관리자 파일 가격 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/files")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {

    private final AdminFilePricingService filePricingService;

    @Operation(summary = "파일 가격 설정", description = """
            파일의 가격을 설정합니다 (유료/무료).

            **설정 옵션**:
            - isPaid: true(유료) / false(무료)
            - price: 가격 (유료인 경우 필수, 크레딧 단위)
            - downloadLimit: 다운로드 제한 횟수 (null이면 무제한)
            - description: 설명

            **예시**:
            - 유료 설정: { "isPaid": true, "price": 5000 }
            - 무료 전환: { "isPaid": false }
            - 제한 설정: { "isPaid": true, "price": 3000, "downloadLimit": 100 }
            """)
    @PostMapping("/{fileUuid}/pricing")
    public ApiResponse<FilePricingResponse> setPricing(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "파일 UUID") @PathVariable UUID fileUuid,
            @Valid @RequestBody FilePricingRequest request) {

        return ApiResponse.success(
                filePricingService.setPricing(fileUuid, request, userDetails.getUsername()));
    }

    @Operation(summary = "파일 가격 정보 조회", description = """
            파일의 가격 정보와 통계를 조회합니다.

            **응답 정보**:
            - 가격 설정 (유료 여부, 가격, 다운로드 제한)
            - 다운로드 통계 (총 다운로드 수, 총 수익)
            """)
    @GetMapping("/{fileUuid}/pricing")
    public ApiResponse<FilePricingResponse> getPricing(
            @Parameter(description = "파일 UUID") @PathVariable UUID fileUuid) {

        return ApiResponse.success(filePricingService.getPricing(fileUuid));
    }

    @Operation(summary = "유료 파일 목록 조회", description = """
            가격이 설정된 유료 파일 목록을 조회합니다.

            **정렬**: 생성일 내림차순

            **응답**: 파일별 가격 정보 및 통계
            """)
    @GetMapping("/paid")
    public ApiResponse<Page<FilePricingResponse>> getPaidFiles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ApiResponse.success(filePricingService.getPaidFiles(pageable));
    }

    @Operation(summary = "파일 무료 전환", description = """
            유료 파일을 무료로 전환합니다.

            **주의**: 이미 구매한 사용자의 내역은 유지됩니다.
            """)
    @PostMapping("/{fileUuid}/pricing/free")
    public ApiResponse<FilePricingResponse> setFree(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "파일 UUID") @PathVariable UUID fileUuid) {

        return ApiResponse.success(
                filePricingService.setFree(fileUuid, userDetails.getUsername()));
    }

    @Operation(summary = "파일 가격 비활성화", description = """
            파일의 가격 설정을 비활성화합니다.

            **효과**: 파일이 무료로 전환됩니다.
            """)
    @DeleteMapping("/{fileUuid}/pricing")
    public ApiResponse<Void> deactivatePricing(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "파일 UUID") @PathVariable UUID fileUuid) {

        filePricingService.deactivatePricing(fileUuid, userDetails.getUsername());
        return ApiResponse.success();
    }
}
