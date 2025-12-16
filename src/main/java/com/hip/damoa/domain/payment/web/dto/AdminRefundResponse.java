package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Refund;
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
 * 관리자용 환불 정보 응답 DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundResponse {

    private UUID refundUuid;
    private UUID paymentUuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
    private String userPhone;
    private BigDecimal refundAmount;
    private String refundReason;
    private String status;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    /**
     * Refund와 UserProfile로 응답 생성
     * 삭제된 User의 경우 안전하게 처리
     */
    public static AdminRefundResponse from(Refund refund, UserProfile profile) {
        UUID userUuid = null;
        String userEmail = null;
        String userName = profile != null ? profile.getName() : null;
        String userPhone = profile != null ? profile.getPhone() : null;

        try {
            User user = refund.getPayment().getUser();
            if (user != null) {
                userUuid = user.getUuid();
                userEmail = user.getEmail();
            }
        } catch (Exception e) {
            log.warn("User not found for refund: {}", refund.getId());
            userEmail = "알 수 없음";
        }

        return AdminRefundResponse.builder()
                .refundUuid(refund.getUuid())
                .paymentUuid(refund.getPayment().getUuid())
                .userUuid(userUuid)
                .userEmail(userEmail)
                .userName(userName)
                .userPhone(userPhone)
                .refundAmount(refund.getRefundAmount())
                .refundReason(refund.getRefundReason())
                .status(refund.getStatus())
                .bankName(refund.getBankName())
                .accountNumber(maskAccountNumber(refund.getAccountNumber()))
                .accountHolder(refund.getAccountHolder())
                .rejectionReason(refund.getRejectionReason())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .build();
    }

    /**
     * Refund만으로 응답 생성 (하위 호환용)
     */
    public static AdminRefundResponse from(Refund refund) {
        return from(refund, null);
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        // 앞 4자리, 뒤 4자리만 표시
        int len = accountNumber.length();
        return accountNumber.substring(0, 4) + "****" + accountNumber.substring(len - 4);
    }
}
