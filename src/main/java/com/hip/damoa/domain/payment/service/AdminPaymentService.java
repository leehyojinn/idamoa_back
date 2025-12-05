package com.hip.damoa.domain.payment.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.payment.model.Payment;
import com.hip.damoa.domain.payment.repository.PaymentRepository;
import com.hip.damoa.domain.payment.web.dto.AdminPaymentResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 결제 관리 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    // ========== 결제 내역 조회 ==========

    /**
     * 모든 결제 내역 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getAllPayments(Pageable pageable) {
        log.info("모든 결제 내역 조회");
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminPaymentResponse::from);
    }

    /**
     * 상태별 결제 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getPaymentsByStatus(String status, Pageable pageable) {
        log.info("상태별 결제 조회: status={}", status);
        return paymentRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(AdminPaymentResponse::from);
    }

    /**
     * 엔티티 타입별 결제 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getPaymentsByEntityType(String entityType, Pageable pageable) {
        log.info("엔티티 타입별 결제 조회: entityType={}", entityType);
        return paymentRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable)
                .map(AdminPaymentResponse::from);
    }

    /**
     * 결제 검색 (키워드)
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> searchPayments(String keyword, Pageable pageable) {
        log.info("결제 검색: keyword={}", keyword);
        return paymentRepository.searchByKeyword(keyword, pageable)
                .map(AdminPaymentResponse::from);
    }

    /**
     * 기간별 결제 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getPaymentsByPeriod(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.info("기간별 결제 조회: startDate={}, endDate={}", startDate, endDate);
        return paymentRepository.findByPeriod(startDate, endDate, pageable)
                .map(AdminPaymentResponse::from);
    }

    /**
     * 결제 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminPaymentResponse getPayment(UUID paymentUuid) {
        log.info("결제 상세 조회: uuid={}", paymentUuid);
        Payment payment = paymentRepository.findByUuid(paymentUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return AdminPaymentResponse.from(payment);
    }

    /**
     * 특정 사용자의 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getUserPayments(UUID userUuid, Pageable pageable) {
        log.info("사용자 결제 내역 조회: userUuid={}", userUuid);

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return paymentRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(AdminPaymentResponse::from);
    }

    // ========== 통계 ==========

    /**
     * 총 결제 금액 (상태별)
     */
    @Transactional(readOnly = true)
    public BigDecimal sumPaymentAmountByStatus(String status) {
        return paymentRepository.sumPaymentAmountByStatus(status);
    }

    /**
     * 완료된 결제 건수
     */
    @Transactional(readOnly = true)
    public long countCompletedPayments() {
        return paymentRepository.countCompletedPayments();
    }
}
