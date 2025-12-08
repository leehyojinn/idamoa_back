package com.hip.damoa.domain.payment.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.payment.service.AdminCreditPackageService;
import com.hip.damoa.domain.payment.web.dto.CreditPackageCreateRequest;
import com.hip.damoa.domain.payment.web.dto.CreditPackageResponse;
import com.hip.damoa.domain.payment.web.dto.CreditPackageUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 관리자용 크레딧 패키지 관리 컨트롤러
 */
@Slf4j
@Tag(name = "1921. Admin Credit Package", description = "관리자 크레딧 패키지 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/credit-packages")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCreditPackageController {

    private final AdminCreditPackageService adminCreditPackageService;

    // ==================== 패키지 조회 ====================

    @Operation(summary = "크레딧 패키지 목록 조회",
            description = """
            크레딧 패키지 목록을 페이징하여 조회합니다.

            **검색 필터**
            - unitAmount: 단위 금액 (10000, 30000, 50000, 100000)
            - isActive: 활성화 여부

            ## 정렬
            - 기본값: displayOrder ASC (순서 오름차순)
            - 사용법: sort=displayOrder,asc 또는 sort=displayOrder,desc
            - 기타 옵션: unitAmount, createdAt
            """)
    @GetMapping
    public ApiResponse<Page<CreditPackageResponse>> getPackages(
            @Parameter(description = "단위 금액 필터") @RequestParam(required = false) Integer unitAmount,
            @Parameter(description = "활성 상태 필터") @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("크레딧 패키지 목록 조회: unitAmount={}, isActive={}", unitAmount, isActive);

        Page<CreditPackageResponse> packages;
        if (unitAmount != null || isActive != null) {
            packages = adminCreditPackageService.searchPackages(unitAmount, isActive, pageable);
        } else {
            packages = adminCreditPackageService.getPackages(pageable);
        }

        return ApiResponse.success(packages);
    }

    @Operation(summary = "크레딧 패키지 상세 조회",
            description = "특정 크레딧 패키지의 상세 정보를 조회합니다.")
    @GetMapping("/{packageUuid}")
    public ApiResponse<CreditPackageResponse> getPackage(
            @Parameter(description = "패키지 UUID") @PathVariable UUID packageUuid) {

        log.info("크레딧 패키지 상세 조회: uuid={}", packageUuid);
        CreditPackageResponse packageResponse = adminCreditPackageService.getPackage(packageUuid);
        return ApiResponse.success(packageResponse);
    }

    @Operation(summary = "활성 패키지 목록 조회 (사용자용)",
            description = """
            사용자에게 노출되는 활성 패키지 목록을 조회합니다.

            displayOrder 순서로 정렬됩니다.
            """)
    @GetMapping("/active")
    public ApiResponse<List<CreditPackageResponse>> getActivePackages() {
        log.info("활성 크레딧 패키지 목록 조회");
        List<CreditPackageResponse> packages = adminCreditPackageService.getActivePackages();
        return ApiResponse.success(packages);
    }


    @Operation(summary = "허용된 단위 금액 목록 조회",
            description = "패키지 생성 시 사용 가능한 단위 금액 목록을 조회합니다.")
    @GetMapping("/unit-amounts")
    public ApiResponse<List<Integer>> getAllowedUnitAmounts() {
        List<Integer> unitAmounts = adminCreditPackageService.getAllowedUnitAmounts();
        return ApiResponse.success(unitAmounts);
    }

    // ==================== 패키지 생성/수정/삭제 ====================

    @Operation(summary = "크레딧 패키지 생성",
            description = """
            새로운 크레딧 패키지를 생성합니다.

            **필수 정보**
            - unitAmount: 단위 금액 (10000, 30000, 50000, 100000)

            **선택 정보**
            - bonusRate: 보너스율 (%) - 3만원 이상만 적용 가능
            - maxBonus: 최대 보너스 한도
            - description: 설명

            **자동 계산**
            - code: KRW_{unitAmount} 형식으로 자동 생성
            - displayName: "X만원권" 형식으로 자동 생성

            **참고**
            - 기본 4개 패키지는 마이그레이션으로 생성됩니다
            - 수량은 사용자가 충전 시 직접 지정합니다
            """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreditPackageResponse> createPackage(
            @Valid @RequestBody CreditPackageCreateRequest request) {

        log.info("크레딧 패키지 생성: unitAmount={}, bonusRate={}", request.getUnitAmount(), request.getBonusRate());
        CreditPackageResponse packageResponse = adminCreditPackageService.createPackage(request);
        return ApiResponse.success(packageResponse);
    }

    @Operation(summary = "크레딧 패키지 수정",
            description = """
            크레딧 패키지 정보를 수정합니다.

            **수정 가능 항목**
            - displayName: 표시 이름
            - bonusRate: 보너스율 (3만원 이상만)
            - maxBonus: 최대 보너스 한도
            - description: 설명

            **수정 불가 항목**
            - unitAmount: 변경 시 새 패키지 생성 필요
            """)
    @PutMapping("/{packageUuid}")
    public ApiResponse<CreditPackageResponse> updatePackage(
            @Parameter(description = "패키지 UUID") @PathVariable UUID packageUuid,
            @Valid @RequestBody CreditPackageUpdateRequest request) {

        log.info("크레딧 패키지 수정: uuid={}", packageUuid);
        CreditPackageResponse packageResponse = adminCreditPackageService.updatePackage(packageUuid, request);
        return ApiResponse.success(packageResponse);
    }

    @Operation(summary = "크레딧 패키지 활성화/비활성화 토글",
            description = """
            크레딧 패키지의 활성화 상태를 토글합니다.

            - 활성화: 사용자에게 노출되어 충전 가능
            - 비활성화: 사용자에게 노출되지 않음
            """)
    @PatchMapping("/{packageUuid}/toggle-active")
    public ApiResponse<CreditPackageResponse> toggleActive(
            @Parameter(description = "패키지 UUID") @PathVariable UUID packageUuid) {

        log.info("크레딧 패키지 활성화 토글: uuid={}", packageUuid);
        CreditPackageResponse packageResponse = adminCreditPackageService.toggleActive(packageUuid);
        return ApiResponse.success(packageResponse);
    }

    @Operation(summary = "크레딧 패키지 삭제",
            description = """
            크레딧 패키지를 삭제합니다. (Soft Delete)

            삭제된 패키지는 사용자에게 노출되지 않습니다.
            """)
    @DeleteMapping("/{packageUuid}")
    public ApiResponse<Void> deletePackage(
            @Parameter(description = "패키지 UUID") @PathVariable UUID packageUuid) {

        log.info("크레딧 패키지 삭제: uuid={}", packageUuid);
        adminCreditPackageService.deletePackage(packageUuid);
        return ApiResponse.success();
    }

    // ==================== 순서 및 보너스 관리 ====================

    @Operation(summary = "패키지 표시 순서 변경",
            description = "패키지의 표시 순서를 변경합니다.")
    @PatchMapping("/{packageUuid}/display-order")
    public ApiResponse<CreditPackageResponse> updateDisplayOrder(
            @Parameter(description = "패키지 UUID") @PathVariable UUID packageUuid,
            @Parameter(description = "새로운 표시 순서") @RequestParam int displayOrder) {

        log.info("크레딧 패키지 순서 변경: uuid={}, displayOrder={}", packageUuid, displayOrder);
        CreditPackageResponse packageResponse = adminCreditPackageService.updateDisplayOrder(packageUuid, displayOrder);
        return ApiResponse.success(packageResponse);
    }

    @Operation(summary = "단위 금액별 보너스율 변경",
            description = """
            특정 단위 금액의 패키지 보너스율을 변경합니다.

            **제한사항**
            - 3만원 이상 패키지만 보너스 적용 가능
            - 1만원권에는 보너스를 적용할 수 없습니다
            """)
    @PatchMapping("/bonus-rate")
    public ApiResponse<CreditPackageResponse> updateBonusRateByUnitAmount(
            @Parameter(description = "단위 금액 (30000, 50000, 100000)") @RequestParam Integer unitAmount,
            @Parameter(description = "새로운 보너스율 (%)") @RequestParam BigDecimal bonusRate,
            @Parameter(description = "최대 보너스 한도 (선택)") @RequestParam(required = false) Integer maxBonus) {

        log.info("보너스율 변경: unitAmount={}, bonusRate={}, maxBonus={}", unitAmount, bonusRate, maxBonus);
        CreditPackageResponse packageResponse = adminCreditPackageService.updateBonusRateByUnitAmount(
                unitAmount, bonusRate, maxBonus);
        return ApiResponse.success(packageResponse);
    }

    // ==================== 통계 ====================

    @Operation(summary = "활성 패키지 수 조회",
            description = "현재 활성화된 크레딧 패키지 수를 조회합니다.")
    @GetMapping("/stats/count")
    public ApiResponse<Long> countActivePackages() {
        long count = adminCreditPackageService.countActivePackages();
        return ApiResponse.success(count);
    }
}
