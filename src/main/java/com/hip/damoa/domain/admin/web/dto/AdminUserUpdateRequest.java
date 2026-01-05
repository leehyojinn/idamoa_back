package com.hip.damoa.domain.admin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 회원 정보 수정 Request
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자용 회원 정보 수정 요청")
public class AdminUserUpdateRequest {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "닉네임", example = "길동이")
    private String nickname;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "소개", example = "안녕하세요")
    private String bio;

    @Schema(description = "주소", example = "서울시 강남구")
    private String address;

    @Schema(description = "우편번호", example = "06134")
    private String postalCode;

    @Schema(description = "프로필 공개 설정 (PUBLIC, PRIVATE, FRIENDS_ONLY)", example = "PUBLIC")
    private String profileVisibility;

    @Schema(description = "상태 (ACTIVE, INACTIVE, SUSPENDED, PENDING)", example = "ACTIVE")
    private String status;

    @Schema(description = "역할 목록", example = "[\"USER\", \"COMPANY\"]")
    private String[] roles;
}
