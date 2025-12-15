package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Credit;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.model.UserStatus;
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

    private UUID userUuid;
    private String userEmail;
    private String userName;
    private String userPhone;
    private UserStatus userStatus;
    private Boolean profileCompleted;
    private Boolean isDeleted;
    private BigDecimal availableCredits;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserCreditResponse from(Credit credit) {
        return AdminUserCreditResponse.builder()
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

    /**
     * User, UserProfile, Credit으로 응답 생성
     * 크레딧이 없는 사용자도 조회 가능하도록 지원
     */
    public static AdminUserCreditResponse fromUser(User user, UserProfile profile, Credit credit) {
        String userName = profile != null ? profile.getName() : null;
        String userPhone = profile != null ? profile.getPhone() : null;

        if (credit != null) {
            return AdminUserCreditResponse.builder()
                    .userUuid(user.getUuid())
                    .userEmail(user.getEmail())
                    .userName(userName)
                    .userPhone(userPhone)
                    .userStatus(user.getStatus())
                    .profileCompleted(user.getProfileCompleted())
                    .isDeleted(user.getIsDeleted())
                    .availableCredits(credit.getAvailableCredits())
                    .totalEarned(credit.getTotalEarned())
                    .totalSpent(credit.getTotalSpent())
                    .createdAt(credit.getCreatedAt())
                    .updatedAt(credit.getUpdatedAt())
                    .build();
        }
        // 크레딧이 없는 사용자
        return AdminUserCreditResponse.builder()
                .userUuid(user.getUuid())
                .userEmail(user.getEmail())
                .userName(userName)
                .userPhone(userPhone)
                .userStatus(user.getStatus())
                .profileCompleted(user.getProfileCompleted())
                .isDeleted(user.getIsDeleted())
                .availableCredits(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
