package com.hip.damoa.domain.payment.service;

import com.hip.damoa.domain.payment.repository.*;
import com.hip.damoa.domain.payment.web.dto.AdminCreditStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 관리자용 크레딧 통계 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCreditStatsService {

    private final CreditRepository creditRepository;
    private final CreditPackageRepository creditPackageRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    /**
     * 종합 크레딧 통계 조회
     */
    @Transactional(readOnly = true)
    public AdminCreditStatsResponse getCreditStats() {
        log.info("종합 크레딧 통계 조회");

        // 크레딧 통계
        BigDecimal totalAvailableCredits = creditRepository.sumTotalAvailableCredits();
        BigDecimal totalEarnedCredits = creditRepository.sumTotalEarnedCredits();
        BigDecimal totalSpentCredits = creditRepository.sumTotalSpentCredits();
        long usersWithCredits = creditRepository.countUsersWithCredits();

        // 결제 통계
        long totalPaymentCount = paymentRepository.countCompletedPayments();
        BigDecimal totalPaymentAmount = paymentRepository.sumPaymentAmountByStatus("COMPLETED");

        // 환불 통계
        long pendingRefundCount = refundRepository.countByStatus("PENDING");
        long completedRefundCount = refundRepository.countByStatus("COMPLETED");
        long rejectedRefundCount = refundRepository.countByStatus("REJECTED");
        BigDecimal totalRefundedAmount = refundRepository.calculateTotalRefundByStatus("COMPLETED");
        BigDecimal pendingRefundAmount = refundRepository.calculateTotalRefundByStatus("PENDING");

        // 패키지 통계
        long activePackageCount = creditPackageRepository.countByIsActiveTrueAndIsDeletedFalse();

        return AdminCreditStatsResponse.builder()
                .totalUsers(usersWithCredits)
                .totalAvailableCredits(totalAvailableCredits != null ? totalAvailableCredits : BigDecimal.ZERO)
                .totalEarnedCredits(totalEarnedCredits != null ? totalEarnedCredits : BigDecimal.ZERO)
                .totalSpentCredits(totalSpentCredits != null ? totalSpentCredits : BigDecimal.ZERO)
                .totalPaymentCount(totalPaymentCount)
                .totalPaymentAmount(totalPaymentAmount != null ? totalPaymentAmount : BigDecimal.ZERO)
                .pendingRefundCount(pendingRefundCount)
                .completedRefundCount(completedRefundCount)
                .rejectedRefundCount(rejectedRefundCount)
                .totalRefundedAmount(totalRefundedAmount != null ? totalRefundedAmount : BigDecimal.ZERO)
                .pendingRefundAmount(pendingRefundAmount != null ? pendingRefundAmount : BigDecimal.ZERO)
                .activePackageCount(activePackageCount)
                .build();
    }

    /**
     * 크레딧 잔액 통계만 조회
     */
    @Transactional(readOnly = true)
    public CreditBalanceStats getCreditBalanceStats() {
        log.info("크레딧 잔액 통계 조회");

        BigDecimal totalAvailable = creditRepository.sumTotalAvailableCredits();
        BigDecimal totalEarned = creditRepository.sumTotalEarnedCredits();
        BigDecimal totalSpent = creditRepository.sumTotalSpentCredits();
        long usersWithCredits = creditRepository.countUsersWithCredits();

        return CreditBalanceStats.builder()
                .totalAvailableCredits(totalAvailable != null ? totalAvailable : BigDecimal.ZERO)
                .totalEarnedCredits(totalEarned != null ? totalEarned : BigDecimal.ZERO)
                .totalSpentCredits(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .usersWithCredits(usersWithCredits)
                .build();
    }

    /**
     * 크레딧 잔액 통계 DTO
     */
    @lombok.Getter
    @lombok.Builder
    public static class CreditBalanceStats {
        private BigDecimal totalAvailableCredits;
        private BigDecimal totalEarnedCredits;
        private BigDecimal totalSpentCredits;
        private long usersWithCredits;
    }
}
