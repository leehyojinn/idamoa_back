package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.admin.service.AdminDashboardService;
import com.hip.damoa.domain.admin.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드 API
 */
@Tag(name = "1900. Admin - Dashboard", description = "관리자 대시보드 통계 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final UserRepository userRepository;

    /**
     * 대시보드 전체 요약 통계 조회
     */
    @Operation(summary = "[관리자] 대시보드 전체 요약 통계 조회",
            description = "관리자 대시보드 메인 화면에 표시될 전체 요약 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/활성 회원 수\n" +
                    "- 오늘/이번 달 신규 회원 수\n" +
                    "- 전체/활성 업체 수\n" +
                    "- 견적요청/제안서 통계\n" +
                    "- 대기중 상담/플래너 신청\n" +
                    "- 문의 통계")
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 대시보드 전체 요약 통계 조회: adminEmail={}", userDetails.getUsername());

        DashboardOverviewResponse response = dashboardService.getOverview();

        return ApiResponse.success(response);
    }

    /**
     * 회원 상세 통계 조회
     */
    @Operation(summary = "[관리자] 회원 상세 통계 조회",
            description = "회원 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 회원 수\n" +
                    "- 상태별 분포 (ACTIVE, PENDING, INACTIVE, SUSPENDED)\n" +
                    "- 역할별 분포 (USER, COMPANY, ADMIN)\n" +
                    "- 인증 현황 (이메일, 휴대폰, 본인인증)\n" +
                    "- 마케팅 동의 수")
    @GetMapping("/users")
    public ApiResponse<UserStatisticsResponse> getUserStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 회원 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        UserStatisticsResponse response = dashboardService.getUserStatistics();

        return ApiResponse.success(response);
    }

    /**
     * 업체 상세 통계 조회
     */
    @Operation(summary = "[관리자] 업체 상세 통계 조회",
            description = "업체 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 업체 수\n" +
                    "- 상태별 분포 (ACTIVE, PENDING, INACTIVE, SUSPENDED)\n" +
                    "- 인증 업체 수\n" +
                    "- 프리미엄 업체 수\n" +
                    "- 프리미엄 등급별 분포 (NONE, BASIC, STANDARD, PREMIUM, VIP)\n" +
                    "- 평균 평점\n" +
                    "- 총 리뷰 수")
    @GetMapping("/companies")
    public ApiResponse<CompanyStatisticsResponse> getCompanyStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 업체 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        CompanyStatisticsResponse response = dashboardService.getCompanyStatistics();

        return ApiResponse.success(response);
    }

    /**
     * 견적요청 상세 통계 조회
     */
    @Operation(summary = "[관리자] 견적요청 상세 통계 조회",
            description = "견적요청 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 견적요청 수\n" +
                    "- 상태별 분포 (DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED)\n" +
                    "- 공개 요청 수\n" +
                    "- 평균 제안서 수\n" +
                    "- 총 조회 수")
    @GetMapping("/estimates")
    public ApiResponse<EstimateStatisticsResponse> getEstimateStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 견적요청 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        EstimateStatisticsResponse response = dashboardService.getEstimateStatistics();

        return ApiResponse.success(response);
    }

    /**
     * 제안서 상세 통계 조회
     */
    @Operation(summary = "[관리자] 제안서 상세 통계 조회",
            description = "제안서 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 제안서 수\n" +
                    "- 상태별 분포 (SUBMITTED, VIEWED, SELECTED, REJECTED, WITHDRAWN)\n" +
                    "- 선택률 (%)\n" +
                    "- 평균 제안 금액")
    @GetMapping("/proposals")
    public ApiResponse<ProposalStatisticsResponse> getProposalStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 제안서 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        ProposalStatisticsResponse response = dashboardService.getProposalStatistics();

        return ApiResponse.success(response);
    }

    /**
     * 빠른상담 상세 통계 조회
     */
    @Operation(summary = "[관리자] 빠른상담 상세 통계 조회",
            description = "빠른상담 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 상담 수\n" +
                    "- 상태별 분포 (SUBMITTED, IN_PROGRESS, COMPLETED, CANCELLED)\n" +
                    "- 회원/비회원 비율\n" +
                    "- 업체 배정 수")
    @GetMapping("/consultations")
    public ApiResponse<ConsultationStatisticsResponse> getConsultationStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 빠른상담 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        ConsultationStatisticsResponse response = dashboardService.getConsultationStatistics();

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청 상세 통계 조회
     */
    @Operation(summary = "[관리자] 플래너 신청 상세 통계 조회",
            description = "플래너 신청 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 신청 수\n" +
                    "- 상태별 분포 (PENDING, IN_PROGRESS, COMPLETED, REJECTED)")
    @GetMapping("/planner-applications")
    public ApiResponse<PlannerApplicationStatisticsResponse> getPlannerApplicationStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 플래너 신청 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        PlannerApplicationStatisticsResponse response = dashboardService.getPlannerApplicationStatistics();

        return ApiResponse.success(response);
    }

    /**
     * 문의 상세 통계 조회
     */
    @Operation(summary = "[관리자] 문의 상세 통계 조회",
            description = "문의 관련 상세 통계를 조회합니다.\n\n" +
                    "**포함 통계**\n" +
                    "- 전체/오늘/이번 달 문의 수\n" +
                    "- 상태별 분포 (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)\n" +
                    "- 문의 유형별 분포")
    @GetMapping("/inquiries")
    public ApiResponse<InquiryStatisticsResponse> getInquiryStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("[관리자] 문의 상세 통계 조회: adminEmail={}", userDetails.getUsername());

        InquiryStatisticsResponse response = dashboardService.getInquiryStatistics();

        return ApiResponse.success(response);
    }
}
