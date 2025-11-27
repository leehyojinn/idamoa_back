package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 회원 상세 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private Long id;
    private UUID uuid;
    private String email;
    private String name;
    private String phoneNumber;
    private String[] roles;
    private String status;

    // 인증 정보
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private Boolean phoneVerified;
    private LocalDateTime phoneVerifiedAt;
    private Boolean identityVerified;

    // 약관 동의
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
    private Boolean marketingAgreed;

    // 프로필 정보
    private Boolean profileCompleted;

    // 로그인 정보
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    private Integer failedLoginCount;

    // 생성/수정 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 삭제 상태 (관리자용)
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    public static AdminUserDetailResponse from(User user, String name, String phoneNumber) {
        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .name(name)
                .phoneNumber(phoneNumber)
                .roles(user.getRoles())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .phoneVerified(user.getPhoneVerified())
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .identityVerified(user.getIdentityVerified())
                .termsAgreed(user.getTermsAgreed())
                .privacyAgreed(user.getPrivacyAgreed())
                .marketingAgreed(user.getMarketingAgreed())
                .profileCompleted(user.getProfileCompleted())
                .lastLoginAt(user.getLastLoginAt())
                .loginCount(user.getLoginCount())
                .failedLoginCount(user.getFailedLoginCount())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isDeleted(user.getIsDeleted())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
