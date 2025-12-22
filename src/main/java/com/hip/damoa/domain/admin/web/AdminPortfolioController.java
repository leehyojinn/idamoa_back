package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.portfolio.service.PortfolioService;
import com.hip.damoa.domain.portfolio.web.dto.PortfolioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 포트폴리오 관리 REST API (관리자용)
 * - 포트폴리오 CRUD
 * - 프로모션 관리는 AdminPortfolioPromotionSettingsController 참조
 */
@Slf4j
@Tag(name = "1903. Admin - Portfolio", description = "포트폴리오 관리 API (관리자용)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/portfolios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPortfolioController {

    private final PortfolioService portfolioService;

    /**
     * 전체 포트폴리오 목록 조회 (관리자용)
     */
    @Operation(
            summary = "전체 포트폴리오 목록 조회 (관리자)",
            description = "삭제되지 않은 모든 포트폴리오 목록을 조회합니다.\n\n" +
                    "**검색 필터 (선택사항)**:\n" +
                    "- `keyword`: 제목, 내용, 업체명 검색\n\n" +
                    "**정렬**:\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=viewCount,desc\n\n" +
                    "**참고**: 프로모션 관리는 `/api/admin/portfolio-promotion-settings/promotions` 참조"
    )
    @GetMapping
    public ApiResponse<Page<PortfolioResponse>> getAllPortfolios(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제목/내용/업체명 검색") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 포트폴리오 목록 조회: adminEmail={}, keyword={}", userDetails.getUsername(), keyword);
        Page<PortfolioResponse> response = portfolioService.searchPortfoliosForAdmin(keyword, pageable);
        return ApiResponse.success(response);
    }

    /**
     * 포트폴리오 상세 조회 (관리자용)
     */
    @Operation(
            summary = "포트폴리오 상세 조회 (관리자)",
            description = "UUID로 포트폴리오 상세 정보를 조회합니다.\n\n" +
                    "**응답 포함 정보**:\n" +
                    "- 기본 정보 (제목, 내용, 썸네일 등)\n" +
                    "- 업체 정보\n" +
                    "- 이미지/영상 목록\n" +
                    "- 필터 옵션\n" +
                    "- 프로모션 정보 (있는 경우)"
    )
    @GetMapping("/{uuid}")
    public ApiResponse<PortfolioResponse> getPortfolio(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {
        log.info("[관리자] 포트폴리오 조회: adminEmail={}, uuid={}", userDetails.getUsername(), uuid);
        PortfolioResponse response = portfolioService.getPortfolioForAdmin(uuid);
        return ApiResponse.success(response);
    }

    /**
     * 포트폴리오 삭제 (관리자용)
     */
    @Operation(
            summary = "포트폴리오 삭제 (관리자)",
            description = "관리자가 포트폴리오를 삭제합니다 (Soft Delete).\n\n" +
                    "**주의**: 연결된 프로모션도 함께 비활성화됩니다."
    )
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deletePortfolio(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {
        log.info("[관리자] 포트폴리오 삭제: adminEmail={}, uuid={}", userDetails.getUsername(), uuid);
        portfolioService.deletePortfolioByAdmin(uuid);
        return ApiResponse.success();
    }
}
