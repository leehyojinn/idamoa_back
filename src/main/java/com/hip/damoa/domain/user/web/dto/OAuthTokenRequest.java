package com.hip.damoa.domain.user.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth 임시 코드를 토큰으로 교환하기 위한 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth 토큰 교환 요청")
public class OAuthTokenRequest {

    @NotBlank(message = "OAuth 코드는 필수입니다")
    @Schema(description = "OAuth 콜백에서 받은 임시 코드", example = "550e8400-e29b-41d4-a716-446655440000")
    private String code;
}
