package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.model.GalleryPromotion;
import com.hip.damoa.domain.board.model.GalleryPromotionTypeSetting;
import com.hip.damoa.domain.board.service.GalleryPromotionService;
import com.hip.damoa.domain.board.service.GalleryPromotionSettingsService;
import com.hip.damoa.domain.board.web.dto.GalleryPromotionResponse;
import com.hip.damoa.domain.board.web.dto.GalleryPromotionTypeSettingCreateRequest;
import com.hip.damoa.domain.board.web.dto.GalleryPromotionTypeSettingResponse;
import com.hip.damoa.domain.board.web.dto.GalleryPromotionTypeSettingUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
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
 * 관리자용 갤러리 우대등록 설정 컨트롤러
 * - 타입별 개별 설정 관리 (확장 가능한 구조)
 */
@Slf4j
@Tag(name = "1940. Admin Gallery Promotion Settings", description = "관리자 갤러리 우대등록 설정 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/gallery-promotion-settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGalleryPromotionSettingsController {

    private final GalleryPromotionSettingsService settingsService;
    private final GalleryPromotionService promotionService;

    // ==================== 설정 목록 조회 ====================

    @Operation(summary = "모든 우대 타입 설정 조회",
            description = """
            모든 갤러리 우대등록 타입 설정을 조회합니다.

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
    public ApiResponse<List<GalleryPromotionTypeSettingResponse>> getAllSettings() {
        log.info("갤러리 우대등록 타입 설정 목록 조회");
        List<GalleryPromotionTypeSetting> settings = settingsService.getAllSettings();
        List<GalleryPromotionTypeSettingResponse> response = settings.stream()
                .map(GalleryPromotionTypeSettingResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    // ==================== 개별 설정 조회 ====================

    @Operation(summary = "특정 우대 타입 설정 조회",
            description = """
            UUID로 특정 우대등록 타입 설정을 조회합니다.
            """)
    @GetMapping("/{uuid}")
    public ApiResponse<GalleryPromotionTypeSettingResponse> getSetting(@PathVariable UUID uuid) {
        log.info("갤러리 우대등록 타입 설정 조회: uuid={}", uuid);
        GalleryPromotionTypeSetting setting = settingsService.getSettingByUuid(uuid);
        return ApiResponse.success(GalleryPromotionTypeSettingResponse.from(setting));
    }

    // ==================== 설정 생성 ====================

    @Operation(summary = "새 우대 타입 생성",
            description = """
            새로운 갤러리 우대등록 타입을 생성합니다.

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
    public ApiResponse<GalleryPromotionTypeSettingResponse> createSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GalleryPromotionTypeSettingCreateRequest request) {

        log.info("갤러리 우대등록 타입 생성: adminEmail={}, promotionType={}",
                userDetails.getUsername(), request.getPromotionType());

        GalleryPromotionTypeSetting setting = settingsService.createSetting(
                userDetails.getUsername(), request);

        return ApiResponse.success(GalleryPromotionTypeSettingResponse.from(setting));
    }

    // ==================== 설정 수정 ====================

    @Operation(summary = "우대 타입 설정 수정",
            description = """
            UUID로 특정 우대등록 타입 설정을 수정합니다.

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
    public ApiResponse<GalleryPromotionTypeSettingResponse> updateSetting(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GalleryPromotionTypeSettingUpdateRequest request) {

        log.info("갤러리 우대등록 타입 수정: adminEmail={}, uuid={}", userDetails.getUsername(), uuid);

        GalleryPromotionTypeSetting setting = settingsService.updateSetting(
                userDetails.getUsername(), uuid, request);

        return ApiResponse.success(GalleryPromotionTypeSettingResponse.from(setting));
    }

    // ==================== 설정 삭제 (비활성화) ====================

    @Operation(summary = "우대 타입 비활성화",
            description = """
            UUID로 특정 우대등록 타입을 비활성화합니다.

            **참고**
            - 비활성화된 타입은 신규 등록에서 선택할 수 없습니다
            - 기존 우대는 만료까지 유지됩니다
            - 완전 삭제가 아닌 비활성화입니다 (복구 가능)
            """)
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deactivateSetting(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("갤러리 우대등록 타입 비활성화: adminEmail={}, uuid={}", userDetails.getUsername(), uuid);

        settingsService.deactivateSetting(userDetails.getUsername(), uuid);

        return ApiResponse.success();
    }

    // ==================== 우대 갤러리 목록 ====================

    @Operation(summary = "활성 우대 갤러리 목록 조회",
            description = """
            현재 활성 상태인 우대 갤러리 목록을 조회합니다.

            **응답 정보**
            - 갤러리 정보 (제목, 이미지 등)
            - 우대 타입 (STANDARD/PREMIUM)
            - 가중치
            - 시작일/종료일
            - 자동갱신 여부
            """)
    @GetMapping("/promotions")
    public ApiResponse<List<GalleryPromotionResponse>> getActivePromotions() {
        log.info("활성 우대 갤러리 목록 조회");
        List<GalleryPromotion> promotions = promotionService.getAllActivePromotions();
        List<GalleryPromotionResponse> response = promotions.stream()
                .map(GalleryPromotionResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }
}
