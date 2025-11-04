package com.hip.damoa.core.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenInfo {
    private String grantType;
    private String accessToken;
    private String refreshToken;

    // 프로필 정보
    private boolean profileCompleted;
    private String currentRole;
}
