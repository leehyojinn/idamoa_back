package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 회원 목록 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {

    private Long id;
    private UUID uuid;
    private String email;
    private String name;
    @Builder.Default
    private String[] roles = new String[0];
    private String status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean identityVerified;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    private LocalDateTime createdAt;
    private Boolean isDeleted;  // 삭제 여부 (관리자용)

    public static AdminUserListResponse from(User user, String name) {
        return AdminUserListResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .name(name)
                .roles(user.getRoles())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .identityVerified(user.getIdentityVerified())
                .lastLoginAt(user.getLastLoginAt())
                .loginCount(user.getLoginCount())
                .createdAt(user.getCreatedAt())
                .isDeleted(user.getIsDeleted())
                .build();
    }
}
