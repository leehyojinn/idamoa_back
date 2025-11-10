package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth Callback 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCallbackRequest {

    @NotBlank(message = "Authorization code는 필수입니다")
    private String code;

    private String state;
}
