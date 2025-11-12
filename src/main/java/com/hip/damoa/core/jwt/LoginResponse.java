package com.hip.damoa.core.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 응답 DTO (Refresh Token은 HttpOnly 쿠키로 전달)
 */
@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String grantType;
    private String accessToken;

    // 프로필 정보
    private boolean profileCompleted;
    private String currentRole;

    /**
     * TokenInfo에서 LoginResponse 생성 (refreshToken 제외)
     */
    public static LoginResponse from(TokenInfo tokenInfo) {
        return LoginResponse.builder()
                .grantType(tokenInfo.getGrantType())
                .accessToken(tokenInfo.getAccessToken())
                .profileCompleted(tokenInfo.isProfileCompleted())
                .currentRole(tokenInfo.getCurrentRole())
                .build();
    }
}
