package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Payment;
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
 * 관리자용 결제 정보 응답 DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentResponse {

    private UUID paymentUuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
    private String userPhone;
    private BigDecimal paymentAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String status;
    private String entityType;
    private Long entityId;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    /**
     * Payment와 UserProfile로 응답 생성
     */
    public static AdminPaymentResponse from(Payment payment, UserProfile profile) {
        UUID userUuid = null;
        String userEmail = null;
        String userName = null;
        String userPhone = null;

        try {
            User user = payment.getUser();
            if (user != null) {
                userUuid = user.getUuid();
                userEmail = user.getEmail();
                if (profile != null) {
                    userName = profile.getName();
                    userPhone = profile.getPhone();
                }
            }
        } catch (Exception e) {
            log.warn("User not found for payment: {}", payment.getId());
            userEmail = "[삭제된 사용자]";
        }

        return AdminPaymentResponse.builder()
                .paymentUuid(payment.getUuid())
                .userUuid(userUuid)
                .userEmail(userEmail)
                .userName(userName)
                .userPhone(userPhone)
                .paymentAmount(payment.getPaymentAmount())
                .feeAmount(payment.getFeeAmount())
                .totalAmount(payment.getTotalAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .entityType(payment.getEntityType())
                .entityId(payment.getEntityId())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }

    /**
     * Payment만으로 응답 생성 (하위 호환용)
     */
    public static AdminPaymentResponse from(Payment payment) {
        return from(payment, null);
    }
}
