package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth Callback 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCallbackResponse {

    private boolean isNewUser;          // 신규 가입 여부
    private TokenInfo tokenInfo;        // JWT 토큰 정보
    private String provider;            // 소셜 로그인 제공자
    private String providerEmail;       // 소셜 계정 이메일
}
