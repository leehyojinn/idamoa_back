package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 현재 로그인한 사용자 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현재 로그인한 사용자 정보 응답")
public class UserInfoResponse {

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "역할 목록", example = "[\"USER\", \"COMPANY\"]")
    @Builder.Default
    private List<String> roles = List.of();

    @Schema(description = "사용자 상태 (ACTIVE, INACTIVE, SUSPENDED)", example = "ACTIVE")
    private String status;

    @Schema(description = "프로필 완성 여부", example = "true")
    private Boolean profileCompleted;

    @Schema(description = "관리자 여부", example = "false")
    private Boolean isAdmin;

    @Schema(description = "일반 사용자 여부", example = "true")
    private Boolean isUser;

    @Schema(description = "업체 사용자 여부", example = "false")
    private Boolean isCompany;

    /**
     * User 엔티티 → UserInfoResponse 변환
     */
    public static UserInfoResponse from(User user) {
        List<String> roleList = Arrays.asList(user.getRoles());

        return UserInfoResponse.builder()
                .email(user.getEmail())
                .roles(roleList)
                .status(user.getStatus().name())  // Enum을 String으로 변환
                .profileCompleted(user.getProfileCompleted())
                .isAdmin(roleList.contains("ADMIN"))
                .isUser(roleList.contains("USER"))
                .isCompany(roleList.contains("COMPANY"))
                .build();
    }
}
