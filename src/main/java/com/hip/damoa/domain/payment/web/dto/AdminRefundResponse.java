package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Refund;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 환불 정보 응답 DTO
 */
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
    private BigDecimal refundAmount;
    private String refundReason;
    private String status;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static AdminRefundResponse from(Refund refund) {
        return AdminRefundResponse.builder()
                .refundUuid(refund.getUuid())
                .paymentUuid(refund.getPayment().getUuid())
                .userUuid(refund.getPayment().getUser().getUuid())
                .userEmail(refund.getPayment().getUser().getEmail())
                .userName(refund.getPayment().getUser().getEmail())
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

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        // 앞 4자리, 뒤 4자리만 표시
        int len = accountNumber.length();
        return accountNumber.substring(0, 4) + "****" + accountNumber.substring(len - 4);
    }
}
