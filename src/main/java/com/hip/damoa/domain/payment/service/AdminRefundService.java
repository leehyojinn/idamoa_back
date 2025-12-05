package com.hip.damoa.domain.payment.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.payment.model.Credit;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.payment.model.Payment;
import com.hip.damoa.domain.payment.model.Refund;
import com.hip.damoa.domain.payment.repository.CreditRepository;
import com.hip.damoa.domain.payment.repository.CreditTransactionRepository;
import com.hip.damoa.domain.payment.repository.PaymentRepository;
import com.hip.damoa.domain.payment.repository.RefundRepository;
import com.hip.damoa.domain.payment.web.dto.AdminRefundRejectRequest;
import com.hip.damoa.domain.payment.web.dto.AdminRefundResponse;
import com.hip.damoa.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 관리자용 환불 관리 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;

    // 트랜잭션 타입 상수
    private static final String TX_REFUND_RESTORE = "REFUND_RESTORE";
    private static final String ENTITY_REFUND = "REFUND";

    // ========== 환불 목록 조회 ==========

    /**
     * 모든 환불 내역 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> getAllRefunds(Pageable pageable) {
        log.info("모든 환불 내역 조회");
        return refundRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminRefundResponse::from);
    }

    /**
     * 상태별 환불 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> getRefundsByStatus(String status, Pageable pageable) {
        log.info("상태별 환불 조회: status={}", status);
        return refundRepository.findByStatus(status, pageable)
                .map(AdminRefundResponse::from);
    }

    /**
     * 대기 중인 환불 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> getPendingRefunds(Pageable pageable) {
        log.info("대기 중인 환불 목록 조회");
        return refundRepository.findPendingRefunds(pageable)
                .map(AdminRefundResponse::from);
    }

    /**
     * 처리 완료된 환불 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> getCompletedRefunds(Pageable pageable) {
        log.info("처리 완료된 환불 목록 조회");
        return refundRepository.findCompletedRefunds(pageable)
                .map(AdminRefundResponse::from);
    }

    /**
     * 환불 검색 (키워드)
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> searchRefunds(String keyword, Pageable pageable) {
        log.info("환불 검색: keyword={}", keyword);
        return refundRepository.searchByKeyword(keyword, pageable)
                .map(AdminRefundResponse::from);
    }

    /**
     * 환불 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminRefundResponse getRefund(UUID refundUuid) {
        log.info("환불 상세 조회: uuid={}", refundUuid);
        Refund refund = refundRepository.findByUuid(refundUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
        return AdminRefundResponse.from(refund);
    }

    // ========== 환불 처리 ==========

    /**
     * 환불 승인
     */
    @Transactional
    public AdminRefundResponse approveRefund(UUID refundUuid) {
        log.info("환불 승인: uuid={}", refundUuid);

        Refund refund = refundRepository.findByUuid(refundUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        // 이미 처리된 환불인지 확인
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }

        // 환불 승인 처리
        refund.complete();
        refund = refundRepository.save(refund);

        // Payment 상태 업데이트
        Payment payment = refund.getPayment();
        payment.complete();
        paymentRepository.save(payment);

        log.info("환불 승인 완료: refundId={}, amount={}", refund.getId(), refund.getRefundAmount());

        return AdminRefundResponse.from(refund);
    }

    /**
     * 환불 거부 (크레딧 복구)
     */
    @Transactional
    public AdminRefundResponse rejectRefund(UUID refundUuid, AdminRefundRejectRequest request) {
        log.info("환불 거부: uuid={}, reason={}", refundUuid, request.getRejectionReason());

        Refund refund = refundRepository.findByUuid(refundUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        // 이미 처리된 환불인지 확인
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }

        Payment payment = refund.getPayment();
        User user = payment.getUser();

        // 환불 거부 시 크레딧 복구
        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_NOT_FOUND));

        // 원래 환불 요청된 금액 (수수료 포함 금액)
        BigDecimal totalAmount = payment.getTotalAmount().abs();

        // 크레딧 복구
        credit.earn(totalAmount);
        creditRepository.save(credit);

        // 거래 내역 기록
        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_REFUND_RESTORE)
                .amount(totalAmount)
                .balanceAfter(credit.getAvailableCredits())
                .reason("[환불 거부 복구] " + request.getRejectionReason())
                .entityType(ENTITY_REFUND)
                .entityId(refund.getId())
                .build();
        transactionRepository.save(transaction);

        // 환불 거부 처리
        refund.reject(request.getRejectionReason());
        refund = refundRepository.save(refund);

        // Payment 상태 업데이트
        payment.fail(request.getRejectionReason());
        paymentRepository.save(payment);

        log.info("환불 거부 완료: refundId={}, restoredAmount={}", refund.getId(), totalAmount);

        return AdminRefundResponse.from(refund);
    }

    // ========== 통계 ==========

    /**
     * 상태별 환불 건수
     */
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return refundRepository.countByStatus(status);
    }

    /**
     * 상태별 총 환불 금액
     */
    @Transactional(readOnly = true)
    public BigDecimal sumRefundAmountByStatus(String status) {
        return refundRepository.calculateTotalRefundByStatus(status);
    }
}
