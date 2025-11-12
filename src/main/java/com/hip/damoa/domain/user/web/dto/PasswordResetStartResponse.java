package com.hip.damoa.domain.user.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 시작 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetStartResponse {

    private String resetToken;
    private String message;
    private Long expiresIn;  // seconds
}
