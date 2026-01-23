package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.service.CompanyDashboardService;
import com.hip.damoa.domain.company.web.dto.dashboard.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 업체 대시보드 컨트롤러
 */
@Tag(name = "1021. Company Dashboard", description = "업체용 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/{companyUuid}/dashboard")
public class CompanyDashboardController {

    private final CompanyDashboardService dashboardService;

    @Operation(summary = "[업체] 대시보드 전체 요약 조회",
            description = "대시보드 메인 화면에 표시할 전체 통계를 한 번에 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 상담신청: 상태별 카운트 (대기중, 처리중, 답변완료, 완료)\n" +
                    "- 리뷰: 총 개수, 평균 평점\n" +
                    "- 견적제안: 총 개수, 수락/거절/대기중 현황\n" +
                    "- 채팅: 활성 채팅방 수, 읽지 않은 메시지 수\n" +
                    "- 업체: 좋아요 수, 조회수\n" +
                    "- 포트폴리오: 총 개수, 총 조회수")
    @GetMapping("/summary")
    public ApiResponse<CompanyDashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        log.info("[업체 대시보드] 전체 요약 조회: userEmail={}, companyUuid={}",
                userDetails.getUsername(), companyUuid);

        CompanyDashboardSummaryResponse summary = dashboardService.getDashboardSummary(
                userDetails.getUsername(), companyUuid);

        return ApiResponse.success(summary);
    }

    @Operation(summary = "[업체] 상담신청 통계 조회",
            description = "상담신청 상세 통계를 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 상태별 카운트 (PENDING, IN_PROGRESS, ANSWERED, COMPLETED, CANCELLED)\n" +
                    "- 최근 상담신청 5개 (UUID, 이름, 제목, 상태, 생성일시)")
    @GetMapping("/consultations")
    public ApiResponse<ConsultationStatsResponse> getConsultationStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        log.info("[업체 대시보드] 상담신청 통계 조회: userEmail={}, companyUuid={}",
                userDetails.getUsername(), companyUuid);

        ConsultationStatsResponse stats = dashboardService.getConsultationStats(
                userDetails.getUsername(), companyUuid);

        return ApiResponse.success(stats);
    }

    @Operation(summary = "[업체] 리뷰 통계 조회",
            description = "리뷰 상세 통계를 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 총 리뷰 수, 평균 평점\n" +
                    "- 점수 분포 (1점~5점별 개수)\n" +
                    "- 최근 리뷰 5개 (UUID, 닉네임, 점수, 내용, 생성일시)")
    @GetMapping("/reviews")
    public ApiResponse<ReviewStatsResponse> getReviewStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        log.info("[업체 대시보드] 리뷰 통계 조회: userEmail={}, companyUuid={}",
                userDetails.getUsername(), companyUuid);

        ReviewStatsResponse stats = dashboardService.getReviewStats(
                userDetails.getUsername(), companyUuid);

        return ApiResponse.success(stats);
    }

    @Operation(summary = "[업체] 견적 제안 통계 조회",
            description = "견적 제안 상세 통계를 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 총 제안 수, 수락/거절/대기중 현황\n" +
                    "- 수락률 (%)\n" +
                    "- 최근 제안 5개 (UUID, 견적요청 제목, 제안금액, 상태, 생성일시)")
    @GetMapping("/estimates")
    public ApiResponse<EstimateStatsResponse> getEstimateStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        log.info("[업체 대시보드] 견적 제안 통계 조회: userEmail={}, companyUuid={}",
                userDetails.getUsername(), companyUuid);

        EstimateStatsResponse stats = dashboardService.getEstimateStats(
                userDetails.getUsername(), companyUuid);

        return ApiResponse.success(stats);
    }

    @Operation(summary = "[업체] 채팅 통계 조회",
            description = "채팅 상세 통계를 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 활성 채팅방 수\n" +
                    "- 읽지 않은 메시지 총 수\n" +
                    "- 최근 채팅방 5개 (UUID, 상대방 이름, 마지막 메시지, 마지막 메시지 시간, 미읽음 수)")
    @GetMapping("/chats")
    public ApiResponse<ChatStatsResponse> getChatStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        log.info("[업체 대시보드] 채팅 통계 조회: userEmail={}, companyUuid={}",
                userDetails.getUsername(), companyUuid);

        ChatStatsResponse stats = dashboardService.getChatStats(
                userDetails.getUsername(), companyUuid);

        return ApiResponse.success(stats);
    }

    @Operation(summary = "[업체] 포트폴리오 통계 조회",
            description = "포트폴리오 상세 통계를 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 총 포트폴리오 수\n" +
                    "- 총 조회수\n" +
                    "- Top 5 포트폴리오 (UUID, 제목, 조회수, 썸네일 URL)")
    @GetMapping("/portfolios")
    public ApiResponse<PortfolioStatsResponse> getPortfolioStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        log.info("[업체 대시보드] 포트폴리오 통계 조회: userEmail={}, companyUuid={}",
                userDetails.getUsername(), companyUuid);

        PortfolioStatsResponse stats = dashboardService.getPortfolioStats(
                userDetails.getUsername(), companyUuid);

        return ApiResponse.success(stats);
    }
}
