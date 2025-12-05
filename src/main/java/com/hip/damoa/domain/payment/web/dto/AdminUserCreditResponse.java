package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Credit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 사용자 크레딧 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserCreditResponse {

    private UUID creditUuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
    private BigDecimal availableCredits;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserCreditResponse from(Credit credit) {
        return AdminUserCreditResponse.builder()
                .creditUuid(credit.getUuid())
                .userUuid(credit.getUser().getUuid())
                .userEmail(credit.getUser().getEmail())
                .userName(credit.getUser().getEmail())
                .availableCredits(credit.getAvailableCredits())
                .totalEarned(credit.getTotalEarned())
                .totalSpent(credit.getTotalSpent())
                .createdAt(credit.getCreatedAt())
                .updatedAt(credit.getUpdatedAt())
                .build();
    }
}
