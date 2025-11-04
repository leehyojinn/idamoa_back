package com.hip.damoa.domain.user.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 상태 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatusResponse {

    private boolean profileCompleted;
    private String profileType;  // "USER_PROFILE" | "COMPANY" | null
    private String currentRole;  // "USER" | "COMPANY"
}
