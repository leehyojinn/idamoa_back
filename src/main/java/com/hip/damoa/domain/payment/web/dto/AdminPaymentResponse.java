package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 결제 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentResponse {

    private UUID paymentUuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
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

    public static AdminPaymentResponse from(Payment payment) {
        return AdminPaymentResponse.builder()
                .paymentUuid(payment.getUuid())
                .userUuid(payment.getUser().getUuid())
                .userEmail(payment.getUser().getEmail())
                .userName(payment.getUser().getEmail())
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
}
