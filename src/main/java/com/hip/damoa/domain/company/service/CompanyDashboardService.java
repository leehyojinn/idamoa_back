package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.repository.CompanyPortfolioRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.repository.CompanyReviewRepository;
import com.hip.damoa.domain.company.web.dto.dashboard.*;
import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import com.hip.damoa.domain.consultation.repository.PortfolioConsultationRepository;
import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import com.hip.damoa.domain.directchat.repository.DirectChatRoomRepository;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.repository.EstimateProposalRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 업체 대시보드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyDashboardService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PortfolioConsultationRepository consultationRepository;
    private final CompanyReviewRepository reviewRepository;
    private final EstimateProposalRepository proposalRepository;
    private final DirectChatRoomRepository chatRoomRepository;
    private final CompanyPortfolioRepository portfolioRepository;

    /**
     * 전체 대시보드 요약 조회
     */
    @Transactional(readOnly = true)
    public CompanyDashboardSummaryResponse getDashboardSummary(String userEmail, UUID companyUuid) {
        log.info("[업체 대시보드] 요약 조회: userEmail={}, companyUuid={}", userEmail, companyUuid);

        Company company = validateAndGetCompany(userEmail, companyUuid);
        Long companyId = company.getId();
        Long ownerId = company.getOwner().getId();

        return CompanyDashboardSummaryResponse.builder()
                .consultation(getConsultationSummary(companyId))
                .review(getReviewSummary(companyId))
                .estimate(getEstimateSummary(companyId))
                .chat(getChatSummary(ownerId))
                .company(CompanyDashboardSummaryResponse.CompanyStats.builder()
                        .likeCount(company.getLikeCount())
                        .viewCount(company.getViewCount())
                        .build())
                .portfolio(getPortfolioSummary(companyId))
                .build();
    }

    /**
     * 상담신청 통계 조회
     */
    @Transactional(readOnly = true)
    public ConsultationStatsResponse getConsultationStats(String userEmail, UUID companyUuid) {
        log.info("[업체 대시보드] 상담신청 통계 조회: userEmail={}, companyUuid={}", userEmail, companyUuid);

        Company company = validateAndGetCompany(userEmail, companyUuid);
        Long companyId = company.getId();

        // 상태별 카운트 조회
        List<Object[]> statusCounts = consultationRepository.countByCompanyIdGroupByStatus(companyId);
        Map<PortfolioConsultationStatus, Long> statusMap = new EnumMap<>(PortfolioConsultationStatus.class);
        for (PortfolioConsultationStatus status : PortfolioConsultationStatus.values()) {
            statusMap.put(status, 0L);
        }
        for (Object[] row : statusCounts) {
            PortfolioConsultationStatus status = (PortfolioConsultationStatus) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count);
        }

        // 최근 상담신청 5개
        List<PortfolioConsultation> recentList = consultationRepository.findRecentByCompanyId(
                companyId, PageRequest.of(0, 5));

        return ConsultationStatsResponse.builder()
                .total(consultationRepository.countByCompanyId(companyId))
                .pending(statusMap.getOrDefault(PortfolioConsultationStatus.PENDING, 0L))
                .inProgress(statusMap.getOrDefault(PortfolioConsultationStatus.IN_PROGRESS, 0L))
                .answered(statusMap.getOrDefault(PortfolioConsultationStatus.ANSWERED, 0L))
                .completed(statusMap.getOrDefault(PortfolioConsultationStatus.COMPLETED, 0L))
                .cancelled(statusMap.getOrDefault(PortfolioConsultationStatus.CANCELLED, 0L))
                .recentConsultations(recentList.stream()
                        .map(ConsultationStatsResponse.RecentConsultation::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 리뷰 통계 조회
     */
    @Transactional(readOnly = true)
    public ReviewStatsResponse getReviewStats(String userEmail, UUID companyUuid) {
        log.info("[업체 대시보드] 리뷰 통계 조회: userEmail={}, companyUuid={}", userEmail, companyUuid);

        Company company = validateAndGetCompany(userEmail, companyUuid);
        Long companyId = company.getId();

        // 점수 분포 조회
        List<Object[]> distribution = reviewRepository.getScoreDistributionByCompanyId(companyId);
        Map<Integer, Long> scoreDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            scoreDistribution.put(i, 0L);
        }
        for (Object[] row : distribution) {
            // rating은 BigDecimal 타입
            BigDecimal scoreDecimal = (BigDecimal) row[0];
            Integer score = scoreDecimal != null ? scoreDecimal.intValue() : 0;
            Long count = (Long) row[1];
            scoreDistribution.put(score, count);
        }

        // 최근 리뷰 5개
        List<CompanyReview> recentList = reviewRepository.findRecentByCompanyId(
                companyId, PageRequest.of(0, 5));

        return ReviewStatsResponse.builder()
                .totalCount(reviewRepository.countPublishedByCompanyId(companyId))
                .averageScore(reviewRepository.getAverageRatingByCompanyId(companyId))
                .scoreDistribution(scoreDistribution)
                .recentReviews(recentList.stream()
                        .map(ReviewStatsResponse.RecentReview::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 견적 제안 통계 조회
     */
    @Transactional(readOnly = true)
    public EstimateStatsResponse getEstimateStats(String userEmail, UUID companyUuid) {
        log.info("[업체 대시보드] 견적 제안 통계 조회: userEmail={}, companyUuid={}", userEmail, companyUuid);

        Company company = validateAndGetCompany(userEmail, companyUuid);
        Long companyId = company.getId();

        // 상태별 카운트 조회
        List<Object[]> statusCounts = proposalRepository.countByCompanyIdGroupByStatus(companyId);
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count);
        }

        long total = proposalRepository.countByCompanyId(companyId);
        long accepted = proposalRepository.countSelectedByCompanyId(companyId);
        long rejected = statusMap.getOrDefault("REJECTED", 0L);
        long pending = statusMap.getOrDefault("SUBMITTED", 0L);

        Double acceptanceRate = total > 0 ? (double) accepted / total * 100 : 0.0;

        // 최근 제안 5개
        List<EstimateProposal> recentList = proposalRepository.findRecentByCompanyId(
                companyId, PageRequest.of(0, 5));

        return EstimateStatsResponse.builder()
                .totalProposals(total)
                .accepted(accepted)
                .rejected(rejected)
                .pending(pending)
                .acceptanceRate(Math.round(acceptanceRate * 10) / 10.0)
                .recentProposals(recentList.stream()
                        .map(EstimateStatsResponse.RecentProposal::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 채팅 통계 조회
     */
    @Transactional(readOnly = true)
    public ChatStatsResponse getChatStats(String userEmail, UUID companyUuid) {
        log.info("[업체 대시보드] 채팅 통계 조회: userEmail={}, companyUuid={}", userEmail, companyUuid);

        Company company = validateAndGetCompany(userEmail, companyUuid);
        Long ownerId = company.getOwner().getId();

        // 활성 채팅방 수
        long activeCount = chatRoomRepository.countActiveByUserId(ownerId);

        // 전체 미읽음 메시지 수
        long totalUnread = chatRoomRepository.getTotalUnreadCountByUserId(ownerId);

        // 채팅방별 미읽음 수 조회
        List<Object[]> unreadCounts = chatRoomRepository.getUnreadCountsByUserId(ownerId);
        Map<Long, Long> unreadMap = new HashMap<>();
        for (Object[] row : unreadCounts) {
            Long roomId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            unreadMap.put(roomId, count);
        }

        // 최근 채팅방 5개
        List<DirectChatRoom> recentList = chatRoomRepository.findRecentByUserId(
                ownerId, PageRequest.of(0, 5));

        return ChatStatsResponse.builder()
                .activeCount(activeCount)
                .unreadCount(totalUnread)
                .recentChats(recentList.stream()
                        .map(room -> ChatStatsResponse.RecentChat.from(
                                room, ownerId, unreadMap.getOrDefault(room.getId(), 0L)))
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 포트폴리오 통계 조회
     */
    @Transactional(readOnly = true)
    public PortfolioStatsResponse getPortfolioStats(String userEmail, UUID companyUuid) {
        log.info("[업체 대시보드] 포트폴리오 통계 조회: userEmail={}, companyUuid={}", userEmail, companyUuid);

        Company company = validateAndGetCompany(userEmail, companyUuid);
        Long companyId = company.getId();

        // Top 5 포트폴리오
        List<CompanyPortfolio> topList = portfolioRepository.findTopByCompanyIdOrderByViewCountDesc(
                companyId, PageRequest.of(0, 5));

        return PortfolioStatsResponse.builder()
                .totalCount(portfolioRepository.countByCompanyId(companyId))
                .totalViews(portfolioRepository.sumViewCountByCompanyId(companyId))
                .topPortfolios(topList.stream()
                        .map(PortfolioStatsResponse.TopPortfolio::from)
                        .collect(Collectors.toList()))
                .build();
    }

    // ===== Private Helper Methods =====

    /**
     * 업체 유효성 검증 및 조회
     */
    private Company validateAndGetCompany(String userEmail, UUID companyUuid) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 소유자 확인
        if (company.getOwner() == null || !company.getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        return company;
    }

    /**
     * 상담신청 요약 조회
     */
    private ConsultationStatsResponse.Summary getConsultationSummary(Long companyId) {
        List<Object[]> statusCounts = consultationRepository.countByCompanyIdGroupByStatus(companyId);
        Map<PortfolioConsultationStatus, Long> statusMap = new EnumMap<>(PortfolioConsultationStatus.class);
        for (PortfolioConsultationStatus status : PortfolioConsultationStatus.values()) {
            statusMap.put(status, 0L);
        }
        for (Object[] row : statusCounts) {
            PortfolioConsultationStatus status = (PortfolioConsultationStatus) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count);
        }

        return ConsultationStatsResponse.Summary.builder()
                .total(consultationRepository.countByCompanyId(companyId))
                .pending(statusMap.getOrDefault(PortfolioConsultationStatus.PENDING, 0L))
                .inProgress(statusMap.getOrDefault(PortfolioConsultationStatus.IN_PROGRESS, 0L))
                .answered(statusMap.getOrDefault(PortfolioConsultationStatus.ANSWERED, 0L))
                .completed(statusMap.getOrDefault(PortfolioConsultationStatus.COMPLETED, 0L))
                .cancelled(statusMap.getOrDefault(PortfolioConsultationStatus.CANCELLED, 0L))
                .build();
    }

    /**
     * 리뷰 요약 조회
     */
    private ReviewStatsResponse.Summary getReviewSummary(Long companyId) {
        return ReviewStatsResponse.Summary.builder()
                .totalCount(reviewRepository.countPublishedByCompanyId(companyId))
                .averageScore(reviewRepository.getAverageRatingByCompanyId(companyId))
                .recentCount(0)  // 추후 최근 7일 리뷰 수 기능 추가 가능
                .build();
    }

    /**
     * 견적 제안 요약 조회
     */
    private EstimateStatsResponse.Summary getEstimateSummary(Long companyId) {
        List<Object[]> statusCounts = proposalRepository.countByCompanyIdGroupByStatus(companyId);
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count);
        }

        return EstimateStatsResponse.Summary.builder()
                .totalProposals(proposalRepository.countByCompanyId(companyId))
                .accepted(proposalRepository.countSelectedByCompanyId(companyId))
                .rejected(statusMap.getOrDefault("REJECTED", 0L))
                .pending(statusMap.getOrDefault("SUBMITTED", 0L))
                .build();
    }

    /**
     * 채팅 요약 조회
     */
    private ChatStatsResponse.Summary getChatSummary(Long ownerId) {
        return ChatStatsResponse.Summary.builder()
                .activeCount(chatRoomRepository.countActiveByUserId(ownerId))
                .unreadCount(chatRoomRepository.getTotalUnreadCountByUserId(ownerId))
                .build();
    }

    /**
     * 포트폴리오 요약 조회
     */
    private PortfolioStatsResponse.Summary getPortfolioSummary(Long companyId) {
        return PortfolioStatsResponse.Summary.builder()
                .totalCount(portfolioRepository.countByCompanyId(companyId))
                .totalViews(portfolioRepository.sumViewCountByCompanyId(companyId))
                .build();
    }
}
