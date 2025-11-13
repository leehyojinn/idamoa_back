package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.domain.user.model.User;
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
public class UserInfoResponse {

    private Long id;
    private String email;
    private List<String> roles;
    private String status;  // UserStatus enum을 String으로 변환
    private Boolean profileCompleted;

    // 편의 메서드
    private Boolean isAdmin;
    private Boolean isUser;
    private Boolean isCompany;

    /**
     * User 엔티티 → UserInfoResponse 변환
     */
    public static UserInfoResponse from(User user) {
        List<String> roleList = Arrays.asList(user.getRoles());

        return UserInfoResponse.builder()
                .id(user.getId())
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
