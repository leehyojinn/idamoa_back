package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.user.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * USER 프로필 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String name;
    private String nickname;
    private String phone;
    private String bio;
    private String avatarUrl;
    private String address;
    private String postalCode;
    private String profileVisibility;

    // 프로필 생성 후 새로운 JWT 토큰 (profileCompleted=true, currentRole 업데이트)
    private TokenInfo tokenInfo;

    /**
     * Entity → DTO 변환
     */
    public static UserProfileResponse from(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .nickname(profile.getNickname())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .profileVisibility(profile.getProfileVisibility())
                .build();
    }

    /**
     * Entity + TokenInfo → DTO 변환
     */
    public static UserProfileResponse from(UserProfile profile, TokenInfo tokenInfo) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .nickname(profile.getNickname())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .profileVisibility(profile.getProfileVisibility())
                .tokenInfo(tokenInfo)
                .build();
    }
}
