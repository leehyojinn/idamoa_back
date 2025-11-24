package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * USER 프로필 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내로 입력해주세요")
    private String name;

    @Size(max = 50, message = "닉네임은 50자 이내로 입력해주세요")
    private String nickname;

    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phone;

    @Size(max = 1000, message = "자기소개는 1000자 이내로 입력해주세요")
    private String bio;

    @Size(max = 500, message = "주소는 500자 이내로 입력해주세요")
    private String address;

    @Size(max = 20, message = "우편번호는 20자 이내로 입력해주세요")
    private String postalCode;

    private String avatarUuid;  // 아바타 파일 UUID (파일 업로드 API로 받은 UUID)

    private String profileVisibility; // PUBLIC, PRIVATE, FRIENDS_ONLY
}
