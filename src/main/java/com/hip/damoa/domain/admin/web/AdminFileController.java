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

@Tag(name = "9931. Admin - File", description = "관리자 파일 가격 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/files")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {

    private final AdminFilePricingService filePricingService;

    @Operation(summary = "파일 가격 설정", description = """
            개별 파일의 가격을 설정합니다.

            ## 사용 시나리오
            1. 사용자가 게시글 생성 시 설정한 가격 수정
            2. 무료 파일을 유료로 변경
            3. 유료 파일을 무료로 변경 (setFree API 권장)

            ## 요청 예시

            ### 유료 설정 (5,000원)
            ```json
            {
              "isPaid": true,
              "price": 5000
            }
            ```

            ### 유료 설정 + 다운로드 제한
            ```json
            {
              "isPaid": true,
              "price": 3000,
              "downloadLimit": 100,
              "description": "한정 100회 다운로드"
            }
            ```

            ### 무료 전환
            ```json
            {
              "isPaid": false
            }
            ```

            ## 주의사항
            - 기존 구매자의 다운로드 권한은 유지됩니다
            - 가격 변경은 새로운 구매에만 적용됩니다
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

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: price, updatedAt

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
