package com.hip.damoa.domain.portfolio.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.portfolio.model.PortfolioPromotion;
import com.hip.damoa.domain.portfolio.model.PortfolioPromotionTypeSetting;
import com.hip.damoa.domain.portfolio.service.PortfolioPromotionService;
import com.hip.damoa.domain.portfolio.service.PortfolioPromotionSettingsService;
import com.hip.damoa.domain.portfolio.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 관리자용 포트폴리오 프로모션 설정 컨트롤러
 */
@Slf4j
@Tag(name = "1903-1. Admin Portfolio Promotion Settings", description = "관리자 포트폴리오 프로모션 설정 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/portfolio-promotion-settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPortfolioPromotionSettingsController {

    private final PortfolioPromotionSettingsService settingsService;
    private final PortfolioPromotionService promotionService;

    // ==================== 설정 목록 조회 ====================

    @Operation(summary = "모든 프로모션 타입 설정 조회",
            description = """
            모든 포트폴리오 프로모션 타입 설정을 조회합니다.

            **응답 정보**
            - uuid: 설정 UUID (API에서 사용)
            - promotionType: 타입 코드 (STANDARD, PREMIUM 등)
            - displayName: 표시명
            - price: 월 가격 (원)
            - weight: 가중치 (노출 확률 배수)
            - displayOrder: 표시 순서
            - isActive: 활성화 여부
            - description: 설명
            """)
    @GetMapping
    public ApiResponse<List<PortfolioPromotionTypeSettingResponse>> getAllSettings() {
        log.info("포트폴리오 프로모션 타입 설정 목록 조회");
        List<PortfolioPromotionTypeSetting> settings = settingsService.getAllSettings();
        List<PortfolioPromotionTypeSettingResponse> response = settings.stream()
                .map(PortfolioPromotionTypeSettingResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    // ==================== 개별 설정 조회 ====================

    @Operation(summary = "특정 프로모션 타입 설정 조회",
            description = "UUID로 특정 프로모션 타입 설정을 조회합니다.")
    @GetMapping("/{uuid}")
    public ApiResponse<PortfolioPromotionTypeSettingResponse> getSetting(@PathVariable UUID uuid) {
        log.info("포트폴리오 프로모션 타입 설정 조회: uuid={}", uuid);
        PortfolioPromotionTypeSetting setting = settingsService.getSettingByUuid(uuid);
        return ApiResponse.success(PortfolioPromotionTypeSettingResponse.from(setting));
    }

    // ==================== 설정 생성 ====================

    @Operation(summary = "새 프로모션 타입 생성",
            description = """
            새로운 포트폴리오 프로모션 타입을 생성합니다.

            **필수 입력**
            - promotionType: 타입 코드 (대문자, 언더스코어만 허용. 예: SUPER_PREMIUM)
            - displayName: 표시명
            - price: 월 가격 (원)
            - weight: 가중치 (1 이상)

            **참고**
            - promotionType은 고유해야 합니다
            - 생성 후 활성화 상태로 시작됩니다
            """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioPromotionTypeSettingResponse> createSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PortfolioPromotionTypeSettingCreateRequest request) {

        log.info("포트폴리오 프로모션 타입 생성: adminEmail={}, promotionType={}",
                userDetails.getUsername(), request.getPromotionType());

        PortfolioPromotionTypeSetting setting = settingsService.createSetting(
                userDetails.getUsername(), request);

        return ApiResponse.success(PortfolioPromotionTypeSettingResponse.from(setting));
    }

    // ==================== 설정 수정 ====================

    @Operation(summary = "프로모션 타입 설정 수정",
            description = """
            UUID로 특정 프로모션 타입 설정을 수정합니다.

            **수정 가능 항목**
            - displayName: 표시명
            - price: 월 가격 (원)
            - weight: 가중치
            - displayOrder: 표시 순서
            - isActive: 활성화 여부
            - description: 설명

            **참고**
            - null로 전달된 필드는 변경되지 않습니다
            - promotionType은 변경할 수 없습니다
            - 설정 변경은 신규 등록부터 적용됩니다 (기존 우대는 영향 없음)
            """)
    @PutMapping("/{uuid}")
    public ApiResponse<PortfolioPromotionTypeSettingResponse> updateSetting(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PortfolioPromotionTypeSettingUpdateRequest request) {

        log.info("포트폴리오 프로모션 타입 수정: adminEmail={}, uuid={}", userDetails.getUsername(), uuid);

        PortfolioPromotionTypeSetting setting = settingsService.updateSetting(
                userDetails.getUsername(), uuid, request);

        return ApiResponse.success(PortfolioPromotionTypeSettingResponse.from(setting));
    }

    // ==================== 설정 삭제 (비활성화) ====================

    @Operation(summary = "프로모션 타입 비활성화",
            description = """
            UUID로 특정 프로모션 타입을 비활성화합니다.

            **참고**
            - 비활성화된 타입은 신규 등록에서 선택할 수 없습니다
            - 기존 우대는 만료까지 유지됩니다
            - 완전 삭제가 아닌 비활성화입니다 (복구 가능)
            """)
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deactivateSetting(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("포트폴리오 프로모션 타입 비활성화: adminEmail={}, uuid={}", userDetails.getUsername(), uuid);

        settingsService.deactivateSetting(userDetails.getUsername(), uuid);

        return ApiResponse.success();
    }

    // ==================== 프로모션 목록 조회 ====================

    @Operation(summary = "프로모션 목록 조회 (상태별)",
            description = """
            포트폴리오 프로모션 목록을 조회합니다.

            **상태 필터 (선택사항)**
            - `status`: 프로모션 상태
              - `ACTIVE`: 현재 활성 (기본값)
              - `EXPIRED`: 만료됨
              - `CANCELLED`: 취소됨
              - 미지정 시: 전체

            **응답 정보**
            - 포트폴리오 정보 (제목 등)
            - 프로모션 타입 (STANDARD/PREMIUM)
            - 가중치
            - 시작일/종료일
            - 자동갱신 여부
            - 상태 (ACTIVE/EXPIRED/CANCELLED)
            """)
    @GetMapping("/promotions")
    public ApiResponse<List<PortfolioPromotionResponse>> getPromotions(
            @Parameter(description = "프로모션 상태 (ACTIVE, EXPIRED, CANCELLED)")
            @RequestParam(required = false) String status) {
        log.info("포트폴리오 프로모션 목록 조회: status={}", status);
        List<PortfolioPromotion> promotions = promotionService.getPromotionsByStatus(status);
        List<PortfolioPromotionResponse> response = promotions.stream()
                .map(PortfolioPromotionResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }
}
