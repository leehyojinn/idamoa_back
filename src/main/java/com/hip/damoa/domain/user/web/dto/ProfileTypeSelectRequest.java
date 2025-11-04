package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 타입 선택 요청 DTO
 * USER_PROFILE 또는 COMPANY 선택
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTypeSelectRequest {

    @NotBlank(message = "프로필 타입은 필수입니다")
    @Pattern(regexp = "^(USER_PROFILE|COMPANY)$",
            message = "프로필 타입은 USER_PROFILE 또는 COMPANY만 가능합니다")
    private String profileType;
}
