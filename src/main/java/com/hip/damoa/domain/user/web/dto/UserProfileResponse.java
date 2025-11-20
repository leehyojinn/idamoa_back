package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.user.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    // 소셜 링크
    private Map<String, Object> socialLinks;

    // 약관 동의
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
    private Boolean marketingAgreed;

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
                .socialLinks(profile.getSocialLinks())
                .termsAgreed(profile.getUser().getTermsAgreed())
                .privacyAgreed(profile.getUser().getPrivacyAgreed())
                .marketingAgreed(profile.getUser().getMarketingAgreed())
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
                .socialLinks(profile.getSocialLinks())
                .termsAgreed(profile.getUser().getTermsAgreed())
                .privacyAgreed(profile.getUser().getPrivacyAgreed())
                .marketingAgreed(profile.getUser().getMarketingAgreed())
                .tokenInfo(tokenInfo)
                .build();
    }
}
