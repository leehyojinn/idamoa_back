package com.hip.damoa.domain.analytics.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.analytics.service.GoogleAnalyticsService;
import com.hip.damoa.domain.analytics.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 - Google Analytics API
 * google.analytics.enabled=true일 때만 활성화됩니다.
 */
@Tag(name = "1900. Admin - Dashboard Analytics", description = "관리자 - Google Analytics 통계 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/analytics")
@ConditionalOnProperty(
        name = "google.analytics.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class AdminAnalyticsController {

    private final GoogleAnalyticsService analyticsService;
    private final UserRepository userRepository;

    /**
     * ADMIN 권한 검증
     */
    private void validateAdmin(UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 일별 트래픽 데이터 조회
     */
    @Operation(summary = "일별 트래픽 조회", description = "기간별 일별 트래픽 데이터를 조회합니다")
    @GetMapping("/daily-traffic")
    public ApiResponse<List<DailyTrafficData>> getDailyTraffic(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsDateRequest request) {

        validateAdmin(userDetails);

        List<DailyTrafficData> result = analyticsService.getDailyTraffic(
                request.getStartDate(), request.getEndDate());

        return ApiResponse.success(result);
    }

    /**
     * 페이지별 조회수 데이터 조회
     */
    @Operation(summary = "페이지별 조회수 조회", description = "기간별 페이지별 조회수 상위 N개를 조회합니다")
    @GetMapping("/page-views")
    public ApiResponse<List<PageViewData>> getPageViews(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsPageRequest request) {

        validateAdmin(userDetails);

        List<PageViewData> result = analyticsService.getPageViews(
                request.getStartDate(), request.getEndDate(), request.getLimit());

        return ApiResponse.success(result);
    }

    /**
     * 특정 페이지 경로 통계 조회
     */
    @Operation(summary = "특정 페이지 통계 조회", description = "특정 페이지 경로의 통계를 조회합니다")
    @GetMapping("/page-stats")
    public ApiResponse<PageViewData> getPageStatsByPath(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute PagePathRequest request) {

        validateAdmin(userDetails);

        PageViewData result = analyticsService.getPageStatsByPath(
                request.getPagePath(), request.getStartDate(), request.getEndDate());

        return ApiResponse.success(result);
    }

    /**
     * 실시간 활성 사용자 수 조회
     */
    @Operation(summary = "실시간 활성 사용자 조회", description = "현재 실시간 활성 사용자 수를 조회합니다")
    @GetMapping("/realtime-users")
    public ApiResponse<RealtimeUsersResponse> getRealtimeUsers(
            @AuthenticationPrincipal UserDetails userDetails) {

        validateAdmin(userDetails);

        RealtimeUsersResponse result = analyticsService.getRealtimeUsers();

        return ApiResponse.success(result);
    }

    /**
     * 요약 통계 조회 (대시보드용)
     */
    @Operation(summary = "요약 통계 조회", description = "메인 대시보드용 요약 통계를 조회합니다")
    @GetMapping("/summary")
    public ApiResponse<SummaryStatsResponse> getSummaryStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsDateRequest request) {

        validateAdmin(userDetails);

        SummaryStatsResponse result = analyticsService.getSummaryStats(
                request.getStartDate(), request.getEndDate());

        return ApiResponse.success(result);
    }

    /**
     * 이벤트별 카운트 데이터 조회
     */
    @Operation(
            summary = "이벤트별 카운트 조회",
            description = "Google Analytics에서 발생한 이벤트 이름별 발생 횟수를 조회합니다.\n" +
                    "이벤트는 page_view (페이지 조회), click (클릭), form_submit (폼 제출) 등 다양한 사용자 행동을 추적합니다.\n" +
                    "기간 내 발생한 이벤트를 카운트별 내림차순으로 정렬하여 반환합니다."
    )
    @GetMapping("/event-counts")
    public ApiResponse<List<EventCountData>> getEventCounts(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsPageRequest request) {

        validateAdmin(userDetails);

        List<EventCountData> result = analyticsService.getEventCounts(
                request.getStartDate(), request.getEndDate(), request.getLimit());

        return ApiResponse.success(result);
    }

    /**
     * 사용자 획득 채널 데이터 조회
     */
    @Operation(
            summary = "사용자 획득 채널 조회",
            description = "사용자가 어떤 경로로 사이트에 유입되었는지를 조회합니다.\n" +
                    "소스/매체별 세션 수, 신규 사용자 수, 참여율을 제공합니다.\n" +
                    "예: google/organic (구글 자연 검색), facebook/social (페이스북 소셜), direct/none (직접 방문) 등\n" +
                    "마케팅 효과를 측정하고 트래픽 소스를 분석하는 데 유용합니다."
    )
    @GetMapping("/acquisition-channels")
    public ApiResponse<List<AcquisitionChannelData>> getAcquisitionChannels(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsPageRequest request) {

        validateAdmin(userDetails);

        List<AcquisitionChannelData> result = analyticsService.getAcquisitionChannels(
                request.getStartDate(), request.getEndDate(), request.getLimit());

        return ApiResponse.success(result);
    }

    /**
     * 기기별 통계 조회
     */
    @Operation(
            summary = "기기별 통계 조회",
            description = "모바일, 데스크톱, 태블릿별로 세션 수, 사용자 수, 이탈률을 조회합니다.\n" +
                    "기기별 사용자 행동 패턴을 분석하고 반응형 디자인 개선에 활용할 수 있습니다.\n" +
                    "세션 수 기준 내림차순으로 정렬됩니다."
    )
    @GetMapping("/device-stats")
    public ApiResponse<List<DeviceStatsData>> getDeviceStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsDateRequest request) {

        validateAdmin(userDetails);

        List<DeviceStatsData> result = analyticsService.getDeviceStats(
                request.getStartDate(), request.getEndDate());

        return ApiResponse.success(result);
    }

    /**
     * 지역별 통계 조회
     */
    @Operation(
            summary = "지역별 통계 조회",
            description = "국가/도시별 세션 수와 사용자 수를 조회합니다.\n" +
                    "사용자의 지리적 분포를 파악하고 지역별 마케팅 전략을 수립하는 데 유용합니다.\n" +
                    "세션 수 기준 내림차순으로 상위 N개 지역을 반환합니다."
    )
    @GetMapping("/location-stats")
    public ApiResponse<List<LocationStatsData>> getLocationStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsPageRequest request) {

        validateAdmin(userDetails);

        List<LocationStatsData> result = analyticsService.getLocationStats(
                request.getStartDate(), request.getEndDate(), request.getLimit());

        return ApiResponse.success(result);
    }

    /**
     * 브라우저별 통계 조회
     */
    @Operation(
            summary = "브라우저별 통계 조회",
            description = "Chrome, Safari, Firefox, Edge 등 브라우저별 세션 수와 사용자 수를 조회합니다.\n" +
                    "브라우저별 호환성 테스트 우선순위를 결정하고 크로스 브라우징 이슈를 파악하는 데 활용됩니다.\n" +
                    "세션 수 기준 내림차순으로 상위 N개 브라우저를 반환합니다."
    )
    @GetMapping("/browser-stats")
    public ApiResponse<List<BrowserStatsData>> getBrowserStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsPageRequest request) {

        validateAdmin(userDetails);

        List<BrowserStatsData> result = analyticsService.getBrowserStats(
                request.getStartDate(), request.getEndDate(), request.getLimit());

        return ApiResponse.success(result);
    }

    /**
     * 참여도 지표 조회
     */
    @Operation(
            summary = "참여도 지표 조회",
            description = "사용자의 사이트 참여도를 나타내는 핵심 지표를 조회합니다.\n" +
                    "- 이탈률: 한 페이지만 보고 떠난 비율 (낮을수록 좋음)\n" +
                    "- 참여율: 10초 이상 머물거나 2개 이상 페이지를 본 세션 비율 (높을수록 좋음)\n" +
                    "- 평균 세션 시간: 사용자가 사이트에 머문 평균 시간\n" +
                    "- 세션당 페이지 뷰: 한 번 방문할 때 평균적으로 본 페이지 수\n" +
                    "이 지표들을 통해 사용자 경험과 콘텐츠 품질을 평가할 수 있습니다."
    )
    @GetMapping("/engagement-metrics")
    public ApiResponse<EngagementMetricsResponse> getEngagementMetrics(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute AnalyticsDateRequest request) {

        validateAdmin(userDetails);

        EngagementMetricsResponse result = analyticsService.getEngagementMetrics(
                request.getStartDate(), request.getEndDate());

        return ApiResponse.success(result);
    }

    /**
     * 기간 비교 통계 조회
     */
    @Operation(
            summary = "기간 비교 통계 조회",
            description = "현재 기간과 이전 기간의 통계를 비교합니다.\n" +
                    "예: 이번 주 vs 지난 주, 오늘 vs 어제, 이번 달 vs 지난 달 등\n" +
                    "두 기간의 활성 사용자, 세션, 페이지 뷰, 이탈률, 참여율, 평균 세션 시간을 비교하여\n" +
                    "트렌드 변화와 성과 개선 여부를 파악할 수 있습니다.\n" +
                    "대시보드에서 기간별 성과를 시각적으로 비교하는 데 유용합니다."
    )
    @GetMapping("/comparison-stats")
    public ApiResponse<ComparisonStatsResponse> getComparisonStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute ComparisonStatsRequest request) {

        validateAdmin(userDetails);

        ComparisonStatsResponse result = analyticsService.getComparisonStats(
                request.getCurrentStart(), request.getCurrentEnd(),
                request.getPreviousStart(), request.getPreviousEnd());

        return ApiResponse.success(result);
    }
}
