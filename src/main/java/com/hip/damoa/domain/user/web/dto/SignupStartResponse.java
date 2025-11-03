package com.hip.damoa.domain.user.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupStartResponse {

    private String signupToken; // Redis에 저장된 토큰
    private String message;
    private Long expiresIn; // seconds (TTL)
}
