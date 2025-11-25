package com.hip.damoa.domain.admin.service;

import com.hip.damoa.domain.admin.web.dto.*;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.consultation.repository.QuickConsultationRepository;
import com.hip.damoa.domain.estimate.repository.EstimateProposalRepository;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.inquiry.repository.InquiryRepository;
import com.hip.damoa.domain.planner.repository.PlannerApplicationRepository;
import com.hip.damoa.domain.user.model.UserStatus;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 대시보드 통계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EstimateRequestRepository estimateRequestRepository;
    private final EstimateProposalRepository estimateProposalRepository;
    private final QuickConsultationRepository quickConsultationRepository;
    private final PlannerApplicationRepository plannerApplicationRepository;
    private final InquiryRepository inquiryRepository;

    /**
     * 대시보드 전체 요약 통계 조회
     */
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        log.info("대시보드 전체 요약 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 회원 통계
        Long totalUsers = userRepository.countByIsDeletedFalse();
        Long activeUsers = userRepository.countByStatusAndIsDeletedFalse(UserStatus.ACTIVE);
        Long newUsersToday = userRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newUsersThisMonth = userRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 업체 통계
        Long totalCompanies = companyRepository.countByIsDeletedFalse();
        Long activeCompanies = companyRepository.countByStatusAndIsDeletedFalse("ACTIVE");

        // 견적요청 통계
        Long totalEstimateRequests = estimateRequestRepository.countByIsDeletedFalse();
        Long estimateRequestsInProgress = estimateRequestRepository.countByStatusAndIsDeletedFalse("IN_PROGRESS");

        // 제안서 통계
        Long totalProposals = estimateProposalRepository.countByIsDeletedFalse();
        Long selectedProposals = estimateProposalRepository.countByIsSelectedTrueAndIsDeletedFalse();
        Double proposalSelectionRate = totalProposals > 0
                ? (double) selectedProposals / totalProposals * 100
                : 0.0;

        // 빠른상담 통계
        Long pendingConsultations = quickConsultationRepository.countByStatusAndIsDeletedFalse("SUBMITTED");

        // 플래너 신청 통계
        Long pendingPlannerApplications = plannerApplicationRepository.countByStatusAndIsDeletedFalse("PENDING");

        // 문의 통계
        Long totalInquiries = inquiryRepository.countByIsDeletedFalse();

        log.info("대시보드 전체 요약 통계 조회 완료");

        return DashboardOverviewResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisMonth(newUsersThisMonth)
                .totalCompanies(totalCompanies)
                .activeCompanies(activeCompanies)
                .totalEstimateRequests(totalEstimateRequests)
                .estimateRequestsInProgress(estimateRequestsInProgress)
                .totalProposals(totalProposals)
                .proposalSelectionRate(Math.round(proposalSelectionRate * 10.0) / 10.0)
                .pendingConsultations(pendingConsultations)
                .pendingPlannerApplications(pendingPlannerApplications)
                .totalInquiries(totalInquiries)
                .build();
    }

    /**
     * 회원 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics() {
        log.info("회원 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = userRepository.countByIsDeletedFalse();
        Long newToday = userRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = userRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("ACTIVE", userRepository.countByStatusAndIsDeletedFalse(UserStatus.ACTIVE));
        byStatus.put("PENDING", userRepository.countByStatusAndIsDeletedFalse(UserStatus.PENDING));
        byStatus.put("INACTIVE", userRepository.countByStatusAndIsDeletedFalse(UserStatus.INACTIVE));
        byStatus.put("SUSPENDED", userRepository.countByStatusAndIsDeletedFalse(UserStatus.SUSPENDED));

        // 역할별 통계 (PostgreSQL array contains)
        Map<String, Long> byRole = new HashMap<>();
        byRole.put("USER", userRepository.countByRolesContainingAndIsDeletedFalse("USER"));
        byRole.put("COMPANY", userRepository.countByRolesContainingAndIsDeletedFalse("COMPANY"));
        byRole.put("ADMIN", userRepository.countByRolesContainingAndIsDeletedFalse("ADMIN"));

        // 인증 현황
        UserStatisticsResponse.VerificationStatistics verification = UserStatisticsResponse.VerificationStatistics.builder()
                .emailVerified(userRepository.countByEmailVerifiedTrueAndIsDeletedFalse())
                .phoneVerified(userRepository.countByPhoneVerifiedTrueAndIsDeletedFalse())
                .identityVerified(userRepository.countByIdentityVerifiedTrueAndIsDeletedFalse())
                .build();

        Long marketingAgreed = userRepository.countByMarketingAgreedTrueAndIsDeletedFalse();

        log.info("회원 상세 통계 조회 완료");

        return UserStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .byRole(byRole)
                .verification(verification)
                .marketingAgreed(marketingAgreed)
                .build();
    }

    /**
     * 업체 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public CompanyStatisticsResponse getCompanyStatistics() {
        log.info("업체 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = companyRepository.countByIsDeletedFalse();
        Long newToday = companyRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = companyRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("ACTIVE", companyRepository.countByStatusAndIsDeletedFalse("ACTIVE"));
        byStatus.put("PENDING", companyRepository.countByStatusAndIsDeletedFalse("PENDING"));
        byStatus.put("INACTIVE", companyRepository.countByStatusAndIsDeletedFalse("INACTIVE"));
        byStatus.put("SUSPENDED", companyRepository.countByStatusAndIsDeletedFalse("SUSPENDED"));

        Long verified = companyRepository.countByVerifiedTrueAndIsDeletedFalse();
        Long premium = companyRepository.countByPremiumUntilAfterAndIsDeletedFalse(LocalDateTime.now());

        // 프리미엄 등급별 통계
        Map<String, Long> byPremiumTier = new HashMap<>();
        byPremiumTier.put("NONE", companyRepository.countByPremiumTierAndIsDeletedFalse("NONE"));
        byPremiumTier.put("BASIC", companyRepository.countByPremiumTierAndIsDeletedFalse("BASIC"));
        byPremiumTier.put("STANDARD", companyRepository.countByPremiumTierAndIsDeletedFalse("STANDARD"));
        byPremiumTier.put("PREMIUM", companyRepository.countByPremiumTierAndIsDeletedFalse("PREMIUM"));
        byPremiumTier.put("VIP", companyRepository.countByPremiumTierAndIsDeletedFalse("VIP"));

        // 평균 평점 및 총 리뷰 수
        var avgRating = companyRepository.getAverageRating();
        var totalReviews = companyRepository.getTotalReviewCount();

        log.info("업체 상세 통계 조회 완료");

        return CompanyStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .verified(verified)
                .premium(premium)
                .byPremiumTier(byPremiumTier)
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .build();
    }

    /**
     * 견적요청 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public EstimateStatisticsResponse getEstimateStatistics() {
        log.info("견적요청 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = estimateRequestRepository.countByIsDeletedFalse();
        Long newToday = estimateRequestRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = estimateRequestRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("DRAFT", estimateRequestRepository.countByStatusAndIsDeletedFalse("DRAFT"));
        byStatus.put("PUBLISHED", estimateRequestRepository.countByStatusAndIsDeletedFalse("PUBLISHED"));
        byStatus.put("IN_PROGRESS", estimateRequestRepository.countByStatusAndIsDeletedFalse("IN_PROGRESS"));
        byStatus.put("MATCHED", estimateRequestRepository.countByStatusAndIsDeletedFalse("MATCHED"));
        byStatus.put("COMPLETED", estimateRequestRepository.countByStatusAndIsDeletedFalse("COMPLETED"));
        byStatus.put("CANCELLED", estimateRequestRepository.countByStatusAndIsDeletedFalse("CANCELLED"));

        Long publicRequests = estimateRequestRepository.countByIsPublicTrueAndIsDeletedFalse();

        // 평균 제안서 수
        Double averageProposalsPerRequest = estimateRequestRepository.getAverageProposalCount();

        // 총 조회 수
        Long totalViews = estimateRequestRepository.getTotalViewCount();

        log.info("견적요청 상세 통계 조회 완료");

        return EstimateStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .publicRequests(publicRequests)
                .averageProposalsPerRequest(averageProposalsPerRequest != null ? Math.round(averageProposalsPerRequest * 10.0) / 10.0 : 0.0)
                .totalViews(totalViews != null ? totalViews : 0L)
                .build();
    }

    /**
     * 제안서 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public ProposalStatisticsResponse getProposalStatistics() {
        log.info("제안서 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = estimateProposalRepository.countByIsDeletedFalse();
        Long newToday = estimateProposalRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = estimateProposalRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("SUBMITTED", estimateProposalRepository.countByStatusAndIsDeletedFalse("SUBMITTED"));
        byStatus.put("VIEWED", estimateProposalRepository.countByStatusAndIsDeletedFalse("VIEWED"));
        byStatus.put("SELECTED", estimateProposalRepository.countByStatusAndIsDeletedFalse("SELECTED"));
        byStatus.put("REJECTED", estimateProposalRepository.countByStatusAndIsDeletedFalse("REJECTED"));
        byStatus.put("WITHDRAWN", estimateProposalRepository.countByStatusAndIsDeletedFalse("WITHDRAWN"));

        // 선택률
        Long selectedProposals = estimateProposalRepository.countByIsSelectedTrueAndIsDeletedFalse();
        Double selectionRate = total > 0 ? (double) selectedProposals / total * 100 : 0.0;

        // 평균 제안 금액
        var averagePrice = estimateProposalRepository.getAveragePrice();

        log.info("제안서 상세 통계 조회 완료");

        return ProposalStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .selectionRate(Math.round(selectionRate * 10.0) / 10.0)
                .averagePrice(averagePrice)
                .build();
    }

    /**
     * 빠른상담 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public ConsultationStatisticsResponse getConsultationStatistics() {
        log.info("빠른상담 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = quickConsultationRepository.countByIsDeletedFalse();
        Long newToday = quickConsultationRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = quickConsultationRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("SUBMITTED", quickConsultationRepository.countByStatusAndIsDeletedFalse("SUBMITTED"));
        byStatus.put("IN_PROGRESS", quickConsultationRepository.countByStatusAndIsDeletedFalse("IN_PROGRESS"));
        byStatus.put("COMPLETED", quickConsultationRepository.countByStatusAndIsDeletedFalse("COMPLETED"));
        byStatus.put("CANCELLED", quickConsultationRepository.countByStatusAndIsDeletedFalse("CANCELLED"));

        // 회원/비회원 비율
        Long member = quickConsultationRepository.countByUserIsNotNullAndIsDeletedFalse();
        Long nonMember = quickConsultationRepository.countByUserIsNullAndIsDeletedFalse();
        ConsultationStatisticsResponse.MemberVsNonMember memberVsNonMember = ConsultationStatisticsResponse.MemberVsNonMember.builder()
                .member(member)
                .nonMember(nonMember)
                .build();

        // 업체 배정 수
        Long assigned = quickConsultationRepository.countByAssignedCompanyIsNotNullAndIsDeletedFalse();

        log.info("빠른상담 상세 통계 조회 완료");

        return ConsultationStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .memberVsNonMember(memberVsNonMember)
                .assigned(assigned)
                .build();
    }

    /**
     * 플래너 신청 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public PlannerApplicationStatisticsResponse getPlannerApplicationStatistics() {
        log.info("플래너 신청 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = plannerApplicationRepository.countByIsDeletedFalse();
        Long newToday = plannerApplicationRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = plannerApplicationRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("PENDING", plannerApplicationRepository.countByStatusAndIsDeletedFalse("PENDING"));
        byStatus.put("IN_PROGRESS", plannerApplicationRepository.countByStatusAndIsDeletedFalse("IN_PROGRESS"));
        byStatus.put("COMPLETED", plannerApplicationRepository.countByStatusAndIsDeletedFalse("COMPLETED"));
        byStatus.put("REJECTED", plannerApplicationRepository.countByStatusAndIsDeletedFalse("REJECTED"));

        log.info("플래너 신청 상세 통계 조회 완료");

        return PlannerApplicationStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .build();
    }

    /**
     * 문의 상세 통계 조회
     */
    @Transactional(readOnly = true)
    public InquiryStatisticsResponse getInquiryStatistics() {
        log.info("문의 상세 통계 조회 시작");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = inquiryRepository.countByIsDeletedFalse();
        Long newToday = inquiryRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);
        Long newThisMonth = inquiryRepository.countByCreatedAtAfterAndIsDeletedFalse(monthStart);

        // 상태별 통계
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("PENDING", inquiryRepository.countByStatusAndIsDeletedFalse("PENDING"));
        byStatus.put("IN_PROGRESS", inquiryRepository.countByStatusAndIsDeletedFalse("IN_PROGRESS"));
        byStatus.put("COMPLETED", inquiryRepository.countByStatusAndIsDeletedFalse("COMPLETED"));
        byStatus.put("CANCELLED", inquiryRepository.countByStatusAndIsDeletedFalse("CANCELLED"));

        // 문의 유형별 통계
        Map<String, Long> byType = inquiryRepository.countGroupByInquiryType();

        log.info("문의 상세 통계 조회 완료");

        return InquiryStatisticsResponse.builder()
                .total(total)
                .newToday(newToday)
                .newThisMonth(newThisMonth)
                .byStatus(byStatus)
                .byType(byType)
                .build();
    }
}
