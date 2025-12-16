package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 거래 내역 응답 DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionResponse {

    private UUID transactionUuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
    private String userPhone;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String reason;
    private String entityType;
    private Long entityId;
    private LocalDateTime createdAt;

    /**
     * CreditTransaction과 UserProfile로 응답 생성
     */
    public static AdminTransactionResponse from(CreditTransaction transaction, UserProfile profile) {
        UUID userUuid = null;
        String userEmail = null;
        String userName = null;
        String userPhone = null;

        try {
            User user = transaction.getUser();
            if (user != null) {
                userUuid = user.getUuid();
                userEmail = user.getEmail();
                if (profile != null) {
                    userName = profile.getName();
                    userPhone = profile.getPhone();
                }
            }
        } catch (Exception e) {
            // User가 삭제된 경우
            log.warn("User not found for transaction: {}", transaction.getId());
            userEmail = "알 수 없음";
        }

        return AdminTransactionResponse.builder()
                .transactionUuid(transaction.getUuid())
                .userUuid(userUuid)
                .userEmail(userEmail)
                .userName(userName)
                .userPhone(userPhone)
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .reason(transaction.getReason())
                .entityType(transaction.getEntityType())
                .entityId(transaction.getEntityId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * CreditTransaction만으로 응답 생성 (하위 호환용)
     */
    public static AdminTransactionResponse from(CreditTransaction transaction) {
        return from(transaction, null);
    }
}
