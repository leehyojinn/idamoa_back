package com.hip.damoa.domain.payment.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.payment.model.Credit;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.payment.repository.CreditRepository;
import com.hip.damoa.domain.payment.repository.CreditTransactionRepository;
import com.hip.damoa.domain.payment.web.dto.AdminCreditAdjustRequest;
import com.hip.damoa.domain.payment.web.dto.AdminTransactionResponse;
import com.hip.damoa.domain.payment.web.dto.AdminUserCreditResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 관리자용 크레딧 관리 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCreditService {

    private final CreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // 트랜잭션 타입 상수
    private static final String TX_ADMIN_GRANT = "ADMIN_GRANT";
    private static final String TX_ADMIN_DEDUCT = "ADMIN_DEDUCT";
    private static final String ENTITY_ADMIN = "ADMIN";

    // ========== 사용자 크레딧 조회 ==========

    /**
     * 모든 사용자의 크레딧 목록 조회 (페이징)
     * 크레딧이 없는 사용자도 포함 (0 크레딧으로 표시)
     * 탈퇴/삭제된 사용자도 필터에 따라 포함
     *
     * @param keyword 검색 키워드 (이메일/이름/전화번호)
     * @param deletedFilter 탈퇴 상태 필터 (ALL: 전체, ACTIVE: 탈퇴안함, DELETED: 탈퇴함)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserCreditResponse> getAllUserCredits(String keyword, String deletedFilter, Pageable pageable) {
        log.info("관리자 사용자 크레딧 조회: keyword={}, deletedFilter={}", keyword, deletedFilter);

        // deletedFilter 값에 따라 isDeleted 조건 결정
        Boolean isDeleted = null; // null = 전체
        if ("ACTIVE".equalsIgnoreCase(deletedFilter)) {
            isDeleted = false;
        } else if ("DELETED".equalsIgnoreCase(deletedFilter)) {
            isDeleted = true;
        }

        // 사용자 검색 (키워드 + 탈퇴 필터)
        Page<User> users = userRepository.searchForAdminCredit(keyword, isDeleted, pageable);

        // 각 사용자의 크레딧과 프로필 조회하여 응답 생성
        return users.map(user -> {
            Credit credit = creditRepository.findByUser(user).orElse(null);
            UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
            return AdminUserCreditResponse.fromUser(user, profile, credit);
        });
    }

    /**
     * 특정 사용자의 크레딧 조회 (UUID)
     * 탈퇴한 사용자도 조회 가능 (관리자용)
     */
    @Transactional(readOnly = true)
    public AdminUserCreditResponse getUserCreditByUuid(UUID userUuid) {
        log.info("사용자 크레딧 조회: userUuid={}", userUuid);

        // 탈퇴한 사용자도 조회 가능
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Credit credit = creditRepository.findByUser(user).orElse(null);
        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);

        return AdminUserCreditResponse.fromUser(user, profile, credit);
    }

    // ========== 수동 크레딧 지급/차감 ==========

    /**
     * 관리자가 사용자에게 크레딧 지급
     */
    @Transactional
    public AdminUserCreditResponse grantCredits(UUID userUuid, AdminCreditAdjustRequest request) {
        log.info("관리자 크레딧 지급: userUuid={}, amount={}, reason={}",
                userUuid, request.getAmount(), request.getReason());

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 크레딧 조회 또는 생성
        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseGet(() -> {
                    Credit newCredit = Credit.builder()
                            .user(user)
                            .build();
                    return creditRepository.save(newCredit);
                });

        // 크레딧 지급
        credit.earn(request.getAmount());
        credit = creditRepository.save(credit);

        // 거래 내역 기록
        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_ADMIN_GRANT)
                .amount(request.getAmount())
                .balanceAfter(credit.getAvailableCredits())
                .reason("[관리자 지급] " + request.getReason())
                .entityType(ENTITY_ADMIN)
                .build();
        transactionRepository.save(transaction);

        log.info("관리자 크레딧 지급 완료: userId={}, amount={}, balance={}",
                user.getId(), request.getAmount(), credit.getAvailableCredits());

        return AdminUserCreditResponse.from(credit);
    }

    /**
     * 관리자가 사용자 크레딧 차감
     */
    @Transactional
    public AdminUserCreditResponse deductCredits(UUID userUuid, AdminCreditAdjustRequest request) {
        log.info("관리자 크레딧 차감: userUuid={}, amount={}, reason={}",
                userUuid, request.getAmount(), request.getReason());

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_NOT_FOUND));

        // 잔액 확인
        if (credit.getAvailableCredits().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        // 크레딧 차감
        credit.spend(request.getAmount());
        credit = creditRepository.save(credit);

        // 거래 내역 기록
        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_ADMIN_DEDUCT)
                .amount(request.getAmount().negate())
                .balanceAfter(credit.getAvailableCredits())
                .reason("[관리자 차감] " + request.getReason())
                .entityType(ENTITY_ADMIN)
                .build();
        transactionRepository.save(transaction);

        log.info("관리자 크레딧 차감 완료: userId={}, amount={}, balance={}",
                user.getId(), request.getAmount(), credit.getAvailableCredits());

        return AdminUserCreditResponse.from(credit);
    }

    // ========== 거래 내역 조회 ==========

    /**
     * 모든 거래 내역 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getAllTransactions(Pageable pageable) {
        log.info("모든 거래 내역 조회");
        return transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminTransactionResponse::from);
    }

    /**
     * 거래 유형별 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getTransactionsByType(String transactionType, Pageable pageable) {
        log.info("거래 유형별 조회: type={}", transactionType);
        return transactionRepository.findByTransactionTypeOrderByCreatedAtDesc(transactionType, pageable)
                .map(AdminTransactionResponse::from);
    }

    /**
     * 거래 내역 검색 (키워드)
     */
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> searchTransactions(String keyword, Pageable pageable) {
        log.info("거래 내역 검색: keyword={}", keyword);
        return transactionRepository.searchByKeyword(keyword, pageable)
                .map(AdminTransactionResponse::from);
    }

    /**
     * 특정 사용자의 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getUserTransactions(UUID userUuid, Pageable pageable) {
        log.info("사용자 거래 내역 조회: userUuid={}", userUuid);

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(AdminTransactionResponse::from);
    }
}
