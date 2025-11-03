package com.hip.damoa.infra.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {
    private String providerId;      // Provider's unique user ID
    private String email;
    private String name;
    private String profileImageUrl;
    private String provider;        // kakao, naver, google
}
