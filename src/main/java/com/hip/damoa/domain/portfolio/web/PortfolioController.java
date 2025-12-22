package com.hip.damoa.domain.portfolio.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.portfolio.model.PortfolioPromotionTypeSetting;
import com.hip.damoa.domain.portfolio.service.*;
import com.hip.damoa.domain.portfolio.web.dto.*;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 포트폴리오 컨트롤러
 */
@Slf4j
@Tag(name = "06-1. Portfolio", description = "포트폴리오 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioBookmarkService bookmarkService;
    private final PortfolioLikeService likeService;
    private final PortfolioPromotionSettingsService settingsService;

    // ==================== 생성 ====================

    @Operation(summary = "포트폴리오 생성",
            description = """
            새 포트폴리오를 생성합니다.

            **이미지 업로드 프로세스:**
            1. /api/files/presigned로 Presigned URL 요청
            2. 응답의 presignedUrl로 S3에 직접 업로드 (PUT)
            3. /api/files/complete 호출하여 업로드 완료 처리
            4. 완료 응답의 uuid를 imageUuids에 포함하여 요청

            **필수 입력:**
            - title: 제목
            - imageUuids: 이미지 UUID 목록 (최소 1개)

            **선택 입력:**
            - thumbnailUuid: 썸네일 이미지 UUID
            - videoUuids: 영상 UUID 목록
            - content: 상세 내용
            - filterOptionIds: 필터 옵션 ID 목록
            - tags: 태그 목록

            **프로모션 옵션:**
            - promotionType: 우대 타입 (STANDARD, PREMIUM)
            - autoRenew: 자동갱신 여부
            """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioResponse> createPortfolio(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PortfolioCreateRequest request) {

        log.info("포트폴리오 생성 요청: userEmail={}", userDetails.getUsername());
        PortfolioResponse response = portfolioService.createPortfolio(userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    // ==================== 조회 ====================

    @Operation(summary = "포트폴리오 상세 조회",
            description = """
            특정 포트폴리오를 조회합니다.

            **응답 포함 정보:**
            - 제목, 내용, 이미지/영상 URL 목록
            - 저작권 정보 (있는 경우)
            - 필터 옵션 정보
            - 태그 목록
            - 회사 정보 (company): 업체명, 전화번호, 평균평점, 리뷰수
            - 리뷰 목록 (reviews): 회사에 대한 승인된 리뷰 내용
            - 로그인한 경우: 북마크/좋아요 여부 포함

            **조회수 증가:**
            - 게시글 조회 시 조회수 자동 증가
            - 동일 사용자 중복 조회도 카운트됨

            **권한:**
            - 비로그인 사용자도 조회 가능
            - 비공개 게시글은 작성자만 조회 가능
            """)
    @GetMapping("/{uuid}")
    public ApiResponse<PortfolioResponse> getPortfolio(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        PortfolioResponse response = portfolioService.getPortfolio(uuid, userEmail);
        return ApiResponse.success(response);
    }

    @Operation(summary = "포트폴리오 검색 (통합)",
            description = """
            포트폴리오 검색 및 목록 조회 통합 API입니다.

            **검색 조건 (모두 선택적, 복합 검색 가능):**
            - keyword: 제목, 내용, 태그, **업체 이름**에서 검색 (LIKE 검색 - 부분 일치)
            - filterOptionIds: 필터 옵션 ID 배열 (예: 업종, 전문영역)
            - companyUuid: 특정 업체의 포트폴리오만 조회 (업체 포트폴리오 보기)
            - onlyBookmarked: true 설정 시 북마크한 포트폴리오만 조회 (로그인 필요)
            - onlyMyPosts: true 설정 시 내가 작성한 포트폴리오만 조회 (로그인 필요)

            **복합 검색 예시:**
            - companyUuid + filterOptionIds: 특정 업체의 포트폴리오 중 필터 조건에 맞는 것
            - keyword + filterOptionIds: 특정 키워드와 필터 옵션을 모두 만족하는 포트폴리오
            - 모든 조건 조합 가능 (AND 연산)

            **페이지네이션:**
            - size: 페이지당 항목 수 (기본 20)
            - page: 페이지 번호 (0부터 시작)
            - sort: 정렬 기준 (기본: createdAt,DESC - 최신순)

            **응답:**
            - 포트폴리오 목록 (제목, 썸네일 이미지, 업체 정보, 조회수, 좋아요 수 등)
            - **업체 정보 (company)**: 업체명, 전화번호
            - 로그인한 경우 각 포트폴리오의 북마크/좋아요 여부 포함
            - 페이지 정보 (totalElements, totalPages 등)

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: viewCount, likeCount

            **활용:**
            - 포트폴리오 메인 페이지
            - 필터링된 포트폴리오 목록
            - 특정 업체 포트폴리오 + 필터 조합
            - 내가 북마크한 포트폴리오
            """)
    @GetMapping("/search")
    public ApiResponse<Page<PortfolioResponse>> searchPortfolios(
            @Parameter(description = "통합 키워드 검색 (제목, 내용, 태그, 업체명)", example = "인테리어")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "필터 옵션 ID 목록", example = "1,2,3")
            @RequestParam(required = false) List<Long> filterOptionIds,

            @Parameter(description = "특정 업체의 포트폴리오만 조회")
            @RequestParam(required = false) UUID companyUuid,

            @Parameter(description = "북마크한 포트폴리오만 조회 (로그인 필요)")
            @RequestParam(required = false, defaultValue = "false") Boolean onlyBookmarked,

            @Parameter(description = "내가 작성한 포트폴리오만 조회 (로그인 필요)")
            @RequestParam(required = false, defaultValue = "false") Boolean onlyMyPosts,

            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PortfolioSearchRequest request = PortfolioSearchRequest.builder()
                .keyword(keyword)
                .filterOptionIds(filterOptionIds)
                .companyUuid(companyUuid)
                .onlyBookmarked(onlyBookmarked)
                .onlyMyPosts(onlyMyPosts)
                .build();

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        Page<PortfolioResponse> response = portfolioService.searchPortfolios(request, userEmail, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "업체별 포트폴리오 조회",
            description = "특정 업체의 공개된 포트폴리오 목록을 조회합니다.")
    @GetMapping("/company/{companyUuid}")
    public ApiResponse<Page<PortfolioResponse>> getPortfoliosByCompany(
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        Page<PortfolioResponse> response = portfolioService.getPortfoliosByCompany(companyUuid, userEmail, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "내 포트폴리오 목록",
            description = "로그인한 사용자의 포트폴리오 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    public ApiResponse<Page<PortfolioResponse>> getMyPortfolios(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PortfolioResponse> response = portfolioService.getMyPortfolios(userDetails.getUsername(), pageable);
        return ApiResponse.success(response);
    }

    // ==================== 수정 ====================

    @Operation(summary = "포트폴리오 수정",
            description = """
            포트폴리오를 수정합니다.

            **프로모션 관리**
            - promotionType: 우대 타입 변경/업그레이드
            - autoRenew: 자동갱신 설정
            - cancelPromotion: true로 설정 시 우대 취소 (자동갱신 해제)
            """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{uuid}")
    public ApiResponse<PortfolioResponse> updatePortfolio(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PortfolioUpdateRequest request) {

        log.info("포트폴리오 수정 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());
        PortfolioResponse response = portfolioService.updatePortfolio(uuid, userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    // ==================== 삭제 ====================

    @Operation(summary = "포트폴리오 삭제",
            description = "포트폴리오를 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deletePortfolio(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("포트폴리오 삭제 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());
        portfolioService.deletePortfolio(uuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    // ==================== 우대 포트폴리오 ====================

    @Operation(summary = "우대 포트폴리오 조회",
            description = """
            가중치 기반 랜덤으로 우대 포트폴리오를 조회합니다.

            **파라미터**
            - filterOptionIds: 필터 옵션 ID 목록 (선택)
            - count: 조회 개수 (기본 8)
            """)
    @GetMapping("/featured")
    public ApiResponse<List<PortfolioResponse>> getFeaturedPortfolios(
            @RequestParam(required = false) List<Long> filterOptionIds,
            @RequestParam(defaultValue = "8") int count,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        List<PortfolioResponse> response = portfolioService.getFeaturedPortfolios(filterOptionIds, count, userEmail);
        return ApiResponse.success(response);
    }

    // ==================== 북마크/좋아요 ====================

    @Operation(summary = "북마크 토글",
            description = "포트폴리오 북마크를 추가/제거합니다. 응답: true=추가됨, false=제거됨",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> toggleBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean result = bookmarkService.toggleBookmark(uuid, userDetails.getUsername());
        return ApiResponse.success(result);
    }

    @Operation(summary = "좋아요 토글",
            description = "포트폴리오 좋아요를 추가/제거합니다. 응답: true=추가됨, false=제거됨",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{uuid}/like")
    public ApiResponse<Boolean> toggleLike(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean result = likeService.toggleLike(uuid, userDetails.getUsername());
        return ApiResponse.success(result);
    }

    // ==================== 프로모션 가격 ====================

    @Operation(summary = "프로모션 가격 조회",
            description = "사용 가능한 프로모션 타입과 가격 목록을 조회합니다.")
    @GetMapping("/promotion-prices")
    public ApiResponse<List<PortfolioPromotionTypeSettingResponse>> getPromotionPrices() {
        List<PortfolioPromotionTypeSetting> settings = settingsService.getActiveSettings();
        List<PortfolioPromotionTypeSettingResponse> response = settings.stream()
                .map(PortfolioPromotionTypeSettingResponse::from)
                .toList();
        return ApiResponse.success(response);
    }
}
